package com.shopflow.inventory;

import com.shopflow.inventory.entity.Inventory;
import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.messaging.InventoryEventProducer;
import com.shopflow.inventory.repository.InventoryRepository;
import com.shopflow.inventory.service.InventoryService;
import com.shopflow.inventory.service.StockCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryEventProducer eventProducer;
    @Mock StockCacheService cacheService;

    @InjectMocks InventoryService inventoryService;

    @Test
    void processOrderCreated_publishesReservedWhenStockSufficient() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Inventory inv = Inventory.builder()
                .productId(productId)
                .productName("Test Product")
                .quantity(10)
                .reservedQuantity(0)
                .build();

        when(inventoryRepository.findAllByProductIdInWithLock(List.of(productId)))
                .thenReturn(List.of(inv));
        when(inventoryRepository.save(any())).thenReturn(inv);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .userEmail("test@example.com")
                .items(List.of(OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(productId)
                        .productName("Test Product")
                        .quantity(3)
                        .unitPrice(BigDecimal.valueOf(10))
                        .build()))
                .build();

        inventoryService.processOrderCreated(event);

        verify(eventProducer).publishInventoryReserved(any());
        verify(eventProducer, never()).publishInventoryFailed(any());
    }

    @Test
    void processOrderCreated_publishesFailedWhenStockInsufficient() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Inventory inv = Inventory.builder()
                .productId(productId)
                .productName("Test Product")
                .quantity(2)
                .reservedQuantity(0)
                .build();

        when(inventoryRepository.findAllByProductIdInWithLock(List.of(productId)))
                .thenReturn(List.of(inv));

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .userEmail("test@example.com")
                .items(List.of(OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(productId)
                        .productName("Test Product")
                        .quantity(5)   // requesting 5, only 2 available
                        .unitPrice(BigDecimal.valueOf(10))
                        .build()))
                .build();

        inventoryService.processOrderCreated(event);

        verify(eventProducer).publishInventoryFailed(any());
        verify(eventProducer, never()).publishInventoryReserved(any());
    }
}
