package com.shopflow.inventory;

import com.shopflow.inventory.dto.InventoryAdjustRequest;
import com.shopflow.inventory.entity.Inventory;
import com.shopflow.inventory.entity.StockReservation;
import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.exception.InvalidStockAdjustmentException;
import com.shopflow.inventory.messaging.InventoryEventProducer;
import com.shopflow.inventory.repository.InventoryRepository;
import com.shopflow.inventory.repository.StockReservationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock StockReservationRepository reservationRepository;
    @Mock InventoryEventProducer eventProducer;
    @Mock StockCacheService cacheService;

    @InjectMocks InventoryService inventoryService;

    private static final UUID PRODUCT = UUID.randomUUID();
    private static final UUID ORDER = UUID.randomUUID();

    private static Inventory stock(int quantity, int reserved) {
        return Inventory.builder().productId(PRODUCT).productName("Test Product")
                .quantity(quantity).reservedQuantity(reserved).build();
    }

    private static OrderCreatedEvent orderFor(int qty) {
        return OrderCreatedEvent.builder()
                .orderId(ORDER)
                .userEmail("test@example.com")
                .items(List.of(OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(PRODUCT).productName("Test Product")
                        .quantity(qty).unitPrice(BigDecimal.TEN).build()))
                .build();
    }

    private static StockReservation reservation(StockReservation.Status status, int qty) {
        return StockReservation.builder().orderId(ORDER).productId(PRODUCT).quantity(qty).status(status).build();
    }

    @Test
    void processOrderCreated_reservesStockAndPublishesReserved() {
        Inventory inv = stock(10, 0);
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(inv));

        inventoryService.processOrderCreated(orderFor(3));

        assertThat(inv.getReservedQuantity()).isEqualTo(3);
        assertThat(inv.getAvailableQuantity()).isEqualTo(7);
        verify(reservationRepository).save(any(StockReservation.class));
        verify(eventProducer).publishInventoryReserved(any());
        verify(eventProducer, never()).publishInventoryFailed(any());
    }

    @Test
    void processOrderCreated_insufficientStock_publishesFailedAndReservesNothing() {
        Inventory inv = stock(2, 0);
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(inv));

        inventoryService.processOrderCreated(orderFor(5));

        assertThat(inv.getReservedQuantity()).isZero();
        verify(reservationRepository, never()).save(any());
        verify(eventProducer).publishInventoryFailed(any());
        verify(eventProducer, never()).publishInventoryReserved(any());
    }

    @Test
    void processOrderCreated_redelivery_doesNotReserveTwice() {
        Inventory inv = stock(10, 3);
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(inv));
        when(reservationRepository.existsByOrderId(ORDER)).thenReturn(true);

        inventoryService.processOrderCreated(orderFor(3));

        assertThat(inv.getReservedQuantity()).isEqualTo(3);   // unchanged
        verify(reservationRepository, never()).save(any());
        verify(eventProducer).publishInventoryReserved(any());   // still tells the order service
    }

    @Test
    void reserve_duplicateLinesForSameProduct_areCheckedAsOneTotal() {
        Inventory inv = stock(5, 0);
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(inv));

        OrderCreatedEvent event = OrderCreatedEvent.builder().orderId(ORDER).items(List.of(
                OrderCreatedEvent.OrderItemEvent.builder().productId(PRODUCT).productName("P").quantity(3).build(),
                OrderCreatedEvent.OrderItemEvent.builder().productId(PRODUCT).productName("P").quantity(3).build()
        )).build();

        assertThat(inventoryService.reserveStockSync(ORDER, event.getItems())).isFalse();
        assertThat(inv.getReservedQuantity()).isZero();
    }

    @Test
    void commitReservation_deductsOnHandAndClearsReserved() {
        Inventory inv = stock(10, 3);
        StockReservation r = reservation(StockReservation.Status.RESERVED, 3);
        when(reservationRepository.findAllByOrderId(ORDER)).thenReturn(List.of(r));
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(inv));

        inventoryService.commitReservation(ORDER);

        assertThat(inv.getQuantity()).isEqualTo(7);
        assertThat(inv.getReservedQuantity()).isZero();
        assertThat(r.getStatus()).isEqualTo(StockReservation.Status.COMMITTED);
    }

    @Test
    void commitReservation_twice_isNoOp() {
        when(reservationRepository.findAllByOrderId(ORDER))
                .thenReturn(List.of(reservation(StockReservation.Status.COMMITTED, 3)));

        inventoryService.commitReservation(ORDER);

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void releaseReservation_beforeCommit_returnsReservedStock() {
        Inventory inv = stock(10, 3);
        StockReservation r = reservation(StockReservation.Status.RESERVED, 3);
        when(reservationRepository.findAllByOrderId(ORDER)).thenReturn(List.of(r));
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(inv));

        inventoryService.releaseReservation(ORDER);

        assertThat(inv.getQuantity()).isEqualTo(10);
        assertThat(inv.getReservedQuantity()).isZero();
        assertThat(r.getStatus()).isEqualTo(StockReservation.Status.RELEASED);
    }

    @Test
    void releaseReservation_afterCommit_putsUnitsBackOnHand() {
        Inventory inv = stock(7, 0);
        StockReservation r = reservation(StockReservation.Status.COMMITTED, 3);
        when(reservationRepository.findAllByOrderId(ORDER)).thenReturn(List.of(r));
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(inv));

        inventoryService.releaseReservation(ORDER);

        assertThat(inv.getQuantity()).isEqualTo(10);
        assertThat(r.getStatus()).isEqualTo(StockReservation.Status.RELEASED);
    }

    @Test
    void releaseReservation_twice_isNoOp() {
        when(reservationRepository.findAllByOrderId(ORDER))
                .thenReturn(List.of(reservation(StockReservation.Status.RELEASED, 3)));

        inventoryService.releaseReservation(ORDER);

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void adjustStock_belowReserved_isRejected() {
        when(inventoryRepository.findAllByProductIdInWithLock(List.of(PRODUCT))).thenReturn(List.of(stock(10, 6)));
        InventoryAdjustRequest request = new InventoryAdjustRequest();
        request.setQuantity(4);

        assertThatThrownBy(() -> inventoryService.adjustStock(PRODUCT, request))
                .isInstanceOf(InvalidStockAdjustmentException.class);
        verify(inventoryRepository, never()).save(any());
    }
}
