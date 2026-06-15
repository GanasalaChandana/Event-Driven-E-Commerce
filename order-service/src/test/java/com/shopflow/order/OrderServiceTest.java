package com.shopflow.order;

import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.messaging.OrderEventProducer;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderEventProducer eventProducer;

    @InjectMocks OrderService orderService;

    @Test
    void placeOrder_savesAsPendingAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(50));

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(item));

        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .userEmail("test@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100))
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(userId, "test@example.com", request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        verify(eventProducer).publishOrderCreated(any());
    }
}
