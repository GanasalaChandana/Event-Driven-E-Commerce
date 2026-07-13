package com.shopflow.order.service;

import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.exception.OrderCancellationException;
import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.messaging.OrderEventProducer;
import com.shopflow.order.repository.OrderRepository;
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

    @InjectMocks OrderService orderService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Test
    void placeOrder_calculatesTotalCorrectly() {
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(UUID.randomUUID());
        item1.setProductName("Headphones");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("49.99"));

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(UUID.randomUUID());
        item2.setProductName("Cable");
        item2.setQuantity(3);
        item2.setUnitPrice(new BigDecimal("9.99"));

        OrderRequest req = new OrderRequest();
        req.setItems(List.of(item1, item2));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order fakeOrder = Order.builder()
                .id(ORDER_ID).userId(USER_ID).userEmail("u@test.com")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("129.95"))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(fakeOrder);

        orderService.placeOrder(USER_ID, "u@test.com", req);

        verify(orderRepository).save(captor.capture());
        // 2×49.99 + 3×9.99 = 99.98 + 29.97 = 129.95
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("129.95");
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void cancelMyOrder_pendingOrder_setsStatusCancelled() {
        Order order = Order.builder()
                .id(ORDER_ID).userId(USER_ID).userEmail("u@test.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelMyOrder(ORDER_ID, USER_ID);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelMyOrder_confirmedOrder_throwsOrderCancellationException() {
        Order order = Order.builder()
                .id(ORDER_ID).userId(USER_ID).userEmail("u@test.com")
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(ORDER_ID, USER_ID))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void cancelMyOrder_orderNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelMyOrder(ORDER_ID, USER_ID))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void confirmOrder_setsStatusConfirmed() {
        Order order = Order.builder()
                .id(ORDER_ID).userId(USER_ID).userEmail("u@test.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.confirmOrder(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(eventProducer).publishOrderConfirmed(any());
    }
}
