package com.shopflow.inventory.service;

import com.shopflow.inventory.dto.InventoryAdjustRequest;
import com.shopflow.inventory.dto.InventoryRequest;
import com.shopflow.inventory.dto.InventoryResponse;
import com.shopflow.inventory.entity.Inventory;
import com.shopflow.inventory.entity.StockReservation;
import com.shopflow.inventory.event.InventoryFailedEvent;
import com.shopflow.inventory.event.InventoryReservedEvent;
import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.exception.InvalidStockAdjustmentException;
import com.shopflow.inventory.exception.InventoryNotFoundException;
import com.shopflow.inventory.messaging.InventoryEventProducer;
import com.shopflow.inventory.repository.InventoryRepository;
import com.shopflow.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stock model: quantity = units on hand, reservedQuantity = units held for orders that are
 * not confirmed yet, available = quantity - reservedQuantity.
 *
 *   reserve  (order placed)    : reserved += n                      → reservation RESERVED
 *   commit   (order confirmed) : reserved -= n, quantity -= n       → COMMITTED
 *   release  (order cancelled) : RESERVED: reserved -= n; COMMITTED: quantity += n → RELEASED
 *
 * Each step is recorded per order in stock_reservations, so repeating it is a no-op.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final InventoryEventProducer eventProducer;
    private final StockCacheService cacheService;

    public enum ReservationOutcome { RESERVED, ALREADY_PROCESSED, INSUFFICIENT_STOCK }

    public record ReservationResult(ReservationOutcome outcome, String reason) {
        public boolean succeeded() {
            return outcome != ReservationOutcome.INSUFFICIENT_STOCK;
        }
    }

    /** Kafka path: order.created → reserve, then publish inventory.reserved / inventory.failed. */
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        ReservationResult result = reserve(event.getOrderId(), event.getItems());
        if (result.succeeded()) {
            eventProducer.publishInventoryReserved(InventoryReservedEvent.builder()
                    .orderId(event.getOrderId())
                    .reservedAt(LocalDateTime.now())
                    .build());
        } else {
            eventProducer.publishInventoryFailed(InventoryFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .reason(result.reason())
                    .build());
        }
    }

    /** Monolith path: reserve in-process. Returns true if stock is (or already was) reserved. */
    @Transactional
    public boolean reserveStockSync(UUID orderId, List<OrderCreatedEvent.OrderItemEvent> items) {
        return reserve(orderId, items).succeeded();
    }

    @Transactional
    public ReservationResult reserve(UUID orderId, List<OrderCreatedEvent.OrderItemEvent> items) {
        Map<UUID, Integer> requested = new LinkedHashMap<>();
        Map<UUID, String> names = new LinkedHashMap<>();
        for (OrderCreatedEvent.OrderItemEvent item : items) {
            requested.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            names.putIfAbsent(item.getProductId(), item.getProductName());
        }

        Map<UUID, Inventory> inventory = lockInventory(requested.keySet());

        // Checked after taking the row locks: a concurrent duplicate delivery for the same
        // order waits on the locks above and then sees the rows the first one inserted.
        if (reservationRepository.existsByOrderId(orderId)) {
            log.info("Stock for order {} was already reserved — skipping", orderId);
            return new ReservationResult(ReservationOutcome.ALREADY_PROCESSED, null);
        }

        for (Map.Entry<UUID, Integer> entry : requested.entrySet()) {
            Inventory inv = inventory.get(entry.getKey());
            if (inv == null || inv.getAvailableQuantity() < entry.getValue()) {
                String reason = inv == null
                        ? "Product not found in inventory: " + entry.getKey()
                        : "Insufficient stock for '" + names.get(entry.getKey()) +
                          "' — requested " + entry.getValue() +
                          ", available " + inv.getAvailableQuantity();
                log.warn("Inventory check failed for order {}: {}", orderId, reason);
                return new ReservationResult(ReservationOutcome.INSUFFICIENT_STOCK, reason);
            }
        }

        for (Map.Entry<UUID, Integer> entry : requested.entrySet()) {
            Inventory inv = inventory.get(entry.getKey());
            inv.setReservedQuantity(inv.getReservedQuantity() + entry.getValue());
            inventoryRepository.save(inv);
            reservationRepository.save(StockReservation.builder()
                    .orderId(orderId)
                    .productId(entry.getKey())
                    .quantity(entry.getValue())
                    .status(StockReservation.Status.RESERVED)
                    .build());
            cacheService.evict(entry.getKey());
        }

        log.info("Inventory reserved for order {}", orderId);
        return new ReservationResult(ReservationOutcome.RESERVED, null);
    }

    /** Order confirmed: turn the reservation into a real deduction. Idempotent. */
    @Transactional
    public void commitReservation(UUID orderId) {
        List<StockReservation> pending = reservationRepository.findAllByOrderId(orderId).stream()
                .filter(r -> r.getStatus() == StockReservation.Status.RESERVED)
                .toList();
        if (pending.isEmpty()) {
            return;
        }

        Map<UUID, Inventory> inventory = lockInventory(pending.stream().map(StockReservation::getProductId).toList());
        for (StockReservation reservation : pending) {
            Inventory inv = inventory.get(reservation.getProductId());
            if (inv != null) {
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - reservation.getQuantity()));
                inv.setQuantity(Math.max(0, inv.getQuantity() - reservation.getQuantity()));
                inventoryRepository.save(inv);
                cacheService.evict(inv.getProductId());
            }
            reservation.setStatus(StockReservation.Status.COMMITTED);
            reservationRepository.save(reservation);
        }
        log.info("Inventory committed for order {}", orderId);
    }

    /** Order cancelled: give the stock back, whether it was only reserved or already committed. Idempotent. */
    @Transactional
    public void releaseReservation(UUID orderId) {
        List<StockReservation> active = reservationRepository.findAllByOrderId(orderId).stream()
                .filter(r -> r.getStatus() != StockReservation.Status.RELEASED)
                .toList();
        if (active.isEmpty()) {
            return;
        }

        Map<UUID, Inventory> inventory = lockInventory(active.stream().map(StockReservation::getProductId).toList());
        for (StockReservation reservation : active) {
            Inventory inv = inventory.get(reservation.getProductId());
            if (inv != null) {
                if (reservation.getStatus() == StockReservation.Status.RESERVED) {
                    inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - reservation.getQuantity()));
                } else {
                    inv.setQuantity(inv.getQuantity() + reservation.getQuantity());
                }
                inventoryRepository.save(inv);
                cacheService.evict(inv.getProductId());
            }
            reservation.setStatus(StockReservation.Status.RELEASED);
            reservationRepository.save(reservation);
        }
        log.info("Inventory released for order {}", orderId);
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

    public Page<InventoryResponse> list(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(InventoryResponse::from);
    }

    @Transactional
    public InventoryResponse adjustStock(UUID productId, InventoryAdjustRequest request) {
        Inventory inv = lockInventory(List.of(productId)).get(productId);
        if (inv == null) {
            throw new InventoryNotFoundException(productId);
        }
        if (request.getQuantity() < inv.getReservedQuantity()) {
            throw new InvalidStockAdjustmentException("Cannot set quantity to " + request.getQuantity()
                    + ": " + inv.getReservedQuantity() + " units are reserved for open orders");
        }
        inv.setQuantity(request.getQuantity());
        inv = inventoryRepository.save(inv);
        cacheService.setStock(inv.getProductId(), inv.getAvailableQuantity());
        log.info("Stock adjusted to {} for product {}", request.getQuantity(), productId);
        return InventoryResponse.from(inv);
    }

    @Transactional
    public InventoryResponse addStock(InventoryRequest request) {
        Inventory inv = lockInventory(List.of(request.getProductId())).get(request.getProductId());
        if (inv == null) {
            inv = Inventory.builder()
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .quantity(0)
                    .reservedQuantity(0)
                    .build();
        }

        inv.setQuantity(inv.getQuantity() + request.getQuantityToAdd());
        inv = inventoryRepository.save(inv);
        cacheService.setStock(inv.getProductId(), inv.getAvailableQuantity());

        log.info("Stock added: {} units for product {}", request.getQuantityToAdd(), request.getProductId());
        return InventoryResponse.from(inv);
    }

    private Map<UUID, Inventory> lockInventory(Collection<UUID> productIds) {
        return inventoryRepository.findAllByProductIdInWithLock(List.copyOf(productIds)).stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));
    }
}
