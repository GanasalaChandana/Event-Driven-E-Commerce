package com.shopflow.order;

import com.shopflow.order.catalog.ProductCatalog;
import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.exception.InvalidOrderStatusTransitionException;
import com.shopflow.order.exception.OrderCancellationException;
import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.exception.ProductUnavailableException;
import com.shopflow.order.messaging.OrderEventProducer;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderEventProducer eventProducer;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock ProductCatalog productCatalog;

    @InjectMocks OrderService orderService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private static OrderItemRequest line(UUID productId, int qty, String clientPrice) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setProductName("client supplied name");
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal(clientPrice));
        return item;
    }

    private static Order order(OrderStatus status) {
        return Order.builder().id(ORDER_ID).userId(USER_ID).userEmail("u@test.com")
                .status(status).totalAmount(BigDecimal.TEN).build();
    }

    @Test
    void placeOrder_usesCatalogPricesNotClientPrices() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        when(productCatalog.findActiveProduct(p1)).thenReturn(
                Optional.of(new ProductCatalog.ProductSnapshot(p1, "Headphones", new BigDecimal("49.99"))));
        when(productCatalog.findActiveProduct(p2)).thenReturn(
                Optional.of(new ProductCatalog.ProductSnapshot(p2, "Cable", new BigDecimal("9.99"))));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest req = new OrderRequest();
        req.setItems(List.of(line(p1, 2, "0.01"), line(p2, 3, "0.01")));   // client claims a 1-cent price

        OrderResponse response = orderService.placeOrder(USER_ID, "u@test.com", req);

        // 2×49.99 + 3×9.99 = 129.95
        assertThat(response.getTotalAmount()).isEqualByComparingTo("129.95");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getItems()).extracting("productName").containsExactly("Headphones", "Cable");
        verify(eventProducer).publishOrderCreated(any());
    }

    @Test
    void placeOrder_mergesDuplicateProductLines() {
        UUID p1 = UUID.randomUUID();
        when(productCatalog.findActiveProduct(p1)).thenReturn(
                Optional.of(new ProductCatalog.ProductSnapshot(p1, "Headphones", new BigDecimal("10.00"))));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest req = new OrderRequest();
        req.setItems(List.of(line(p1, 1, "10"), line(p1, 2, "10")));

        OrderResponse response = orderService.placeOrder(USER_ID, "u@test.com", req);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void placeOrder_unknownProduct_isRejectedAndNothingSaved() {
        UUID p1 = UUID.randomUUID();
        when(productCatalog.findActiveProduct(p1)).thenReturn(Optional.empty());

        OrderRequest req = new OrderRequest();
        req.setItems(List.of(line(p1, 1, "10")));

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, "u@test.com", req))
                .isInstanceOf(ProductUnavailableException.class);
        verify(orderRepository, never()).save(any());
        verify(eventProducer, never()).publishOrderCreated(any());
    }

    @Test
    void confirmOrder_pending_becomesConfirmedAndPublishes() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(o));

        assertThat(orderService.confirmOrder(ORDER_ID)).isTrue();

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(eventProducer).publishOrderConfirmed(any());
    }

    @Test
    void confirmOrder_redeliveredOrAlreadyCancelled_isIgnored() {
        Order o = order(OrderStatus.CANCELLED);
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(o));

        assertThat(orderService.confirmOrder(ORDER_ID)).isFalse();

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventProducer, never()).publishOrderConfirmed(any());
    }

    @Test
    void cancelOrder_onlyAffectsPendingOrders() {
        Order confirmed = order(OrderStatus.CONFIRMED);
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(confirmed));

        assertThat(orderService.cancelOrder(ORDER_ID, "Insufficient stock")).isFalse();

        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(eventProducer, never()).publishOrderCancelled(any());
    }

    @Test
    void handleInventoryReserved_forOrderCancelledMeanwhile_releasesStock() {
        Order cancelled = order(OrderStatus.CANCELLED);
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(cancelled));

        orderService.handleInventoryReserved(ORDER_ID);

        verify(eventProducer).publishInventoryRelease(any());
        verify(eventProducer, never()).publishOrderConfirmed(any());
    }

    @Test
    void cancelMyOrder_pending_cancelsAndReleasesStock() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelMyOrder(ORDER_ID, USER_ID);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(eventProducer).publishInventoryRelease(any());
        verify(eventProducer).publishOrderCancelled(any());
    }

    @Test
    void cancelMyOrder_confirmed_throws() {
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));

        assertThatThrownBy(() -> orderService.cancelMyOrder(ORDER_ID, USER_ID))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void cancelMyOrder_someoneElsesOrder_isNotFound() {
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> orderService.cancelMyOrder(ORDER_ID, UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateOrderStatus_cancelledIsTerminal() {
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));

        assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_ID, OrderStatus.CONFIRMED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void updateOrderStatus_adminCancelOfConfirmed_releasesStock() {
        Order o = order(OrderStatus.CONFIRMED);
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateOrderStatus(ORDER_ID, OrderStatus.CANCELLED);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventProducer).publishInventoryRelease(any());
        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(published.capture());
    }

    @Test
    void updateOrderStatus_sameStatus_isNoOp() {
        when(orderRepository.findWithLockById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));

        orderService.updateOrderStatus(ORDER_ID, OrderStatus.CONFIRMED);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(eventProducer);
    }
}
