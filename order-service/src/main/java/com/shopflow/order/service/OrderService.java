package com.shopflow.order.service;

import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderItem;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.event.OrderCancelledEvent;
import com.shopflow.order.event.OrderConfirmedEvent;
import com.shopflow.order.event.OrderCreatedEvent;
import com.shopflow.order.event.OrderPlacedApplicationEvent;
import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.messaging.OrderEventProducer;
import com.shopflow.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public OrderResponse placeOrder(UUID userId, String userEmail, OrderRequest request) {
        // Build order items and calculate total
        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .userEmail(userEmail)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .build();

        // Link items to order
        items.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order saved = orderRepository.save(order);
        log.info("Order {} placed by user {} — PENDING", saved.getId(), userId);

        // Publish Spring application event — picked up by SyncOrderFulfillmentService
        // in the monolith for in-JVM confirmation when Kafka is unavailable
        applicationEventPublisher.publishEvent(
                new OrderPlacedApplicationEvent(this, saved.getId(), userEmail, total, request));

        // Publish to Kafka — Inventory Service will pick this up when Kafka is available
        eventProducer.publishOrderCreated(OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .userId(userId)
                .userEmail(userEmail)
                .totalAmount(total)
                .createdAt(LocalDateTime.now())
                .items(request.getItems().stream()
                        .map(i -> OrderCreatedEvent.OrderItemEvent.builder()
                                .productId(i.getProductId())
                                .productName(i.getProductName())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .build())
                        .toList())
                .build());

        return OrderResponse.from(saved);
    }

    // Called by Kafka consumer when Inventory Service confirms stock
    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order {} CONFIRMED", orderId);

        eventProducer.publishOrderConfirmed(OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .totalAmount(order.getTotalAmount())
                .confirmedAt(LocalDateTime.now())
                .build());
    }

    // Called by Kafka consumer when Inventory Service reports insufficient stock
    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} CANCELLED — {}", orderId, reason);

        eventProducer.publishOrderCancelled(OrderCancelledEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .reason(reason)
                .cancelledAt(LocalDateTime.now())
                .build());
    }

    public Page<OrderResponse> getMyOrders(UUID userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable).map(OrderResponse::from);
    }

    public OrderResponse getMyOrder(UUID orderId, UUID userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    public Page<OrderResponse> getAllOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findAllByStatus(status, pageable).map(OrderResponse::from);
    }

    public OrderResponse getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
