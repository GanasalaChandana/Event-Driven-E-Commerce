package com.shopflow.inventory.service;

import com.shopflow.inventory.dto.InventoryRequest;
import com.shopflow.inventory.dto.InventoryResponse;
import com.shopflow.inventory.entity.Inventory;
import com.shopflow.inventory.event.InventoryFailedEvent;
import com.shopflow.inventory.event.InventoryReservedEvent;
import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.exception.InventoryNotFoundException;
import com.shopflow.inventory.messaging.InventoryEventProducer;
import com.shopflow.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer eventProducer;
    private final StockCacheService cacheService;

    /**
     * Core saga step: called when order.created arrives.
     * Uses pessimistic locking to prevent race conditions on concurrent orders.
     */
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        List<UUID> productIds = event.getItems().stream()
                .map(OrderCreatedEvent.OrderItemEvent::getProductId)
                .toList();

        // Lock all relevant rows in one query
        Map<UUID, Inventory> inventoryMap = inventoryRepository
                .findAllByProductIdInWithLock(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        // Check every item has sufficient available stock
        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            Inventory inv = inventoryMap.get(item.getProductId());
            if (inv == null || inv.getAvailableQuantity() < item.getQuantity()) {
                String reason = inv == null
                        ? "Product not found in inventory: " + item.getProductId()
                        : "Insufficient stock for '" + item.getProductName() +
                          "' — requested " + item.getQuantity() +
                          ", available " + inv.getAvailableQuantity();

                log.warn("Inventory check failed for order {}: {}", event.getOrderId(), reason);
                eventProducer.publishInventoryFailed(InventoryFailedEvent.builder()
                        .orderId(event.getOrderId())
                        .reason(reason)
                        .build());
                return;
            }
        }

        // All items available — reserve stock
        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            Inventory inv = inventoryMap.get(item.getProductId());
            inv.setReservedQuantity(inv.getReservedQuantity() + item.getQuantity());
            inventoryRepository.save(inv);
            // Evict cache so next read gets fresh value
            cacheService.evict(item.getProductId());
        }

        log.info("Inventory reserved for order {}", event.getOrderId());
        eventProducer.publishInventoryReserved(InventoryReservedEvent.builder()
                .orderId(event.getOrderId())
                .reservedAt(LocalDateTime.now())
                .build());
    }

    public InventoryResponse getByProductId(UUID productId) {
        // Cache-aside: check Redis first
        Integer cached = cacheService.getStock(productId);
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        if (cached == null) {
            cacheService.setStock(productId, inv.getAvailableQuantity());
        }
        return InventoryResponse.from(inv);
    }

    @Transactional
    public InventoryResponse addStock(InventoryRequest request) {
        Inventory inv = inventoryRepository.findByProductId(request.getProductId())
                .orElseGet(() -> Inventory.builder()
                        .productId(request.getProductId())
                        .productName(request.getProductName())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        inv.setQuantity(inv.getQuantity() + request.getQuantityToAdd());
        inv = inventoryRepository.save(inv);
        cacheService.setStock(inv.getProductId(), inv.getAvailableQuantity());

        log.info("Stock added: {} units for product {}", request.getQuantityToAdd(), request.getProductId());
        return InventoryResponse.from(inv);
    }
}
