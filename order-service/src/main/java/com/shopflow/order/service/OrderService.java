package com.shopflow.order.service;

import com.shopflow.order.catalog.ProductCatalog;
import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderItem;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.event.InventoryReleaseEvent;
import com.shopflow.order.event.OrderCancelledApplicationEvent;
import com.shopflow.order.event.OrderCancelledEvent;
import com.shopflow.order.event.OrderConfirmedEvent;
import com.shopflow.order.event.OrderCreatedEvent;
import com.shopflow.order.event.OrderPlacedApplicationEvent;
import com.shopflow.order.event.OrderRefundApplicationEvent;
import com.shopflow.order.event.OrderStatusChangedApplicationEvent;
import com.shopflow.order.exception.InvalidOrderStatusTransitionException;
import com.shopflow.order.exception.OrderCancellationException;
import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.exception.ProductUnavailableException;
import com.shopflow.order.messaging.OrderEventProducer;
import com.shopflow.order.payment.PaymentProperties;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Order lifecycle: PENDING → CONFIRMED → (CANCELLED), or PENDING → CANCELLED.
 * CANCELLED is terminal. Every transition takes a row lock and is a no-op when the
 * order is no longer in the expected state, so redelivered events are harmless.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProductCatalog productCatalog;
    private final PaymentProperties paymentProperties;

    @Transactional
    public OrderResponse placeOrder(UUID userId, String userEmail, OrderRequest request) {
        // Merge duplicate lines for the same product so stock checks see the real total
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest line : request.getItems()) {
            quantities.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }

        // Names and prices always come from the catalog — never from the client
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            ProductCatalog.ProductSnapshot product = productCatalog.findActiveProduct(entry.getKey())
                    .orElseThrow(() -> new ProductUnavailableException(entry.getKey()));
            items.add(OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .quantity(entry.getValue())
                    .unitPrice(product.price())
                    .build());
        }

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .userEmail(userEmail)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .build();

        items.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order saved = orderRepository.save(order);
        log.info("Order {} placed by user {} — PENDING", saved.getId(), userId);

        List<OrderCreatedEvent.OrderItemEvent> itemEvents = items.stream()
                .map(i -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        // In-JVM event — the monolith fulfils the order synchronously after commit
        applicationEventPublisher.publishEvent(
                new OrderPlacedApplicationEvent(this, saved.getId(), userEmail, total, itemEvents));

        // Kafka event (sent after commit) — Inventory Service reserves stock
        eventProducer.publishOrderCreated(OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .userId(userId)
                .userEmail(userEmail)
                .totalAmount(total)
                .createdAt(LocalDateTime.now())
                .items(itemEvents)
                .build());

        return OrderResponse.from(saved);
    }

    /** Stock was reserved: PENDING → CONFIRMED. Returns false if the order was no longer PENDING. */
    @Transactional
    public boolean confirmOrder(UUID orderId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} is {} — ignoring confirmation", orderId, order.getStatus());
            return false;
        }

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
        return true;
    }

    /**
     * Kafka: inventory.reserved. Confirms the order, or — if the customer cancelled while
     * the reservation was in flight — asks Inventory Service to give the stock back.
     */
    @Transactional
    public void handleInventoryReserved(UUID orderId) {
        // With payments enabled the order stays PENDING (stock reserved) until the customer pays
        if (!paymentProperties.enabled() && confirmOrder(orderId)) {
            return;
        }
        Order order = lockOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            eventProducer.publishInventoryRelease(InventoryReleaseEvent.builder()
                    .orderId(orderId)
                    .reason("Order was cancelled before stock was reserved")
                    .build());
        }
    }

    /** Payment succeeded: PENDING → CONFIRMED. Returns false if the order was no longer PENDING. */
    @Transactional
    public boolean confirmPaidOrder(UUID orderId, String paymentIntentId) {
        if (!confirmOrder(orderId)) {
            return false;
        }
        Order order = lockOrder(orderId);
        order.setPaymentIntentId(paymentIntentId);
        orderRepository.save(order);
        // Lets the monolith commit the reserved stock and send the confirmation email
        applicationEventPublisher.publishEvent(
                new OrderStatusChangedApplicationEvent(this, orderId, order.getUserEmail(), OrderStatus.CONFIRMED));
        return true;
    }

    /** No payment arrived in time: PENDING → CANCELLED and the reserved stock is released. */
    @Transactional
    public boolean cancelUnpaidOrder(UUID orderId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            return false;
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        eventProducer.publishOrderCancelled(cancelledEvent(order, "Payment not completed in time"));
        eventProducer.publishInventoryRelease(InventoryReleaseEvent.builder()
                .orderId(orderId).reason("Payment not completed in time").build());
        applicationEventPublisher.publishEvent(
                new OrderCancelledApplicationEvent(this, order.getId(), order.getUserEmail()));
        return true;
    }

    /** Stock could not be reserved: PENDING → CANCELLED. Returns false if the order was no longer PENDING. */
    @Transactional
    public boolean cancelOrder(UUID orderId, String reason) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} is {} — ignoring cancellation ({})", orderId, order.getStatus(), reason);
            return false;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} CANCELLED — {}", orderId, reason);

        eventProducer.publishOrderCancelled(cancelledEvent(order, reason));
        return true;
    }

    public Page<OrderResponse> getMyOrders(UUID userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable).map(OrderResponse::from);
    }

    public OrderResponse getMyOrder(UUID orderId, UUID userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public OrderResponse cancelMyOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findWithLockById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderCancellationException(orderId,
                    "only PENDING orders can be cancelled (current status: " + order.getStatus() + ")");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} CANCELLED by user {}", orderId, userId);

        eventProducer.publishOrderCancelled(cancelledEvent(order, "Cancelled by customer"));
        eventProducer.publishInventoryRelease(InventoryReleaseEvent.builder()
                .orderId(orderId).reason("Cancelled by customer").build());

        applicationEventPublisher.publishEvent(
                new OrderCancelledApplicationEvent(this, order.getId(), order.getUserEmail()));

        return OrderResponse.from(order);
    }

    /**
     * Admin override. Allowed: PENDING → CONFIRMED, PENDING/CONFIRMED → CANCELLED.
     * Setting the current status again is a no-op; nothing leaves CANCELLED.
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = lockOrder(orderId);
        OrderStatus current = order.getStatus();

        if (current == newStatus) {
            return OrderResponse.from(order);
        }
        if (current == OrderStatus.CANCELLED || newStatus == OrderStatus.PENDING) {
            throw new InvalidOrderStatusTransitionException(orderId, current, newStatus);
        }

        boolean refundPayment = current == OrderStatus.CONFIRMED && newStatus == OrderStatus.CANCELLED
                && order.getPaymentIntentId() != null;

        order.setStatus(newStatus);
        orderRepository.save(order);
        log.info("Order {} status updated {} -> {} by admin", orderId, current, newStatus);

        if (newStatus == OrderStatus.CONFIRMED) {
            eventProducer.publishOrderConfirmed(OrderConfirmedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .userEmail(order.getUserEmail())
                    .totalAmount(order.getTotalAmount())
                    .confirmedAt(LocalDateTime.now())
                    .build());
        } else {
            eventProducer.publishOrderCancelled(cancelledEvent(order, "Cancelled by admin"));
            eventProducer.publishInventoryRelease(InventoryReleaseEvent.builder()
                    .orderId(orderId).reason("Cancelled by admin").build());
            if (refundPayment) {
                applicationEventPublisher.publishEvent(
                        new OrderRefundApplicationEvent(this, orderId, order.getPaymentIntentId()));
            }
        }

        applicationEventPublisher.publishEvent(
                new OrderStatusChangedApplicationEvent(this, order.getId(), order.getUserEmail(), newStatus));

        return OrderResponse.from(order);
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

    private Order lockOrder(UUID orderId) {
        return orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private static OrderCancelledEvent cancelledEvent(Order order, String reason) {
        return OrderCancelledEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .reason(reason)
                .cancelledAt(LocalDateTime.now())
                .build();
    }
}
