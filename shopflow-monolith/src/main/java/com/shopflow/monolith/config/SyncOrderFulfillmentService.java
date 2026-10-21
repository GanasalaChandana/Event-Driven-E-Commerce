package com.shopflow.monolith.config;

import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.service.InventoryService;
import com.shopflow.order.event.OrderCancelledApplicationEvent;
import com.shopflow.order.event.OrderPlacedApplicationEvent;
import com.shopflow.order.event.OrderStatusChangedApplicationEvent;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.payment.PaymentProperties;
import com.shopflow.order.service.OrderService;
import com.shopflow.monolith.notification.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * Runs the order saga in-process for the monolith deployment:
 * reserve stock → confirm (and commit the reservation) or cancel; release stock on cancellation.
 * Every inventory step is idempotent, so a parallel Kafka path cannot double-count stock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrderFulfillmentService {

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final EmailNotificationService emailNotificationService;
    private final PaymentProperties paymentProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedApplicationEvent event) {
        UUID orderId = event.getOrderId();
        log.info("Sync fulfillment triggered for order {}", orderId);

        List<OrderCreatedEvent.OrderItemEvent> items = event.getItems().stream()
                .map(i -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        try {
            if (inventoryService.reserveStockSync(orderId, items)) {
                if (paymentProperties.enabled()) {
                    // Stock is held; the order is confirmed by the Stripe webhook once it is paid
                    if ("CANCELLED".equals(orderService.getOrderById(orderId).getStatus())) {
                        inventoryService.releaseReservation(orderId);
                    }
                    log.info("Order {} stock reserved — awaiting payment", orderId);
                    return;
                }
                if (orderService.confirmOrder(orderId)) {
                    inventoryService.commitReservation(orderId);
                    log.info("Order {} CONFIRMED via sync fulfillment", orderId);
                    emailNotificationService.sendOrderConfirmation(
                            orderId, event.getUserEmail(), event.getTotalAmount());
                } else {
                    // The customer cancelled while we were reserving — give the stock back
                    inventoryService.releaseReservation(orderId);
                    log.info("Order {} was cancelled during fulfillment — stock released", orderId);
                }
            } else if (orderService.cancelOrder(orderId, "Insufficient stock")) {
                log.warn("Order {} CANCELLED via sync fulfillment — insufficient stock", orderId);
                emailNotificationService.sendOrderCancellation(
                        orderId, event.getUserEmail(), "Insufficient stock");
            }
        } catch (Exception e) {
            log.error("Sync fulfillment failed for order {}", orderId, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelledByUser(OrderCancelledApplicationEvent event) {
        try {
            inventoryService.releaseReservation(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to release stock for cancelled order {}", event.getOrderId(), e);
        }
        log.info("Sending cancellation email for order {} cancelled by customer", event.getOrderId());
        emailNotificationService.sendOrderCancellation(
                event.getOrderId(), event.getUserEmail(), "Cancelled by customer");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminStatusChange(OrderStatusChangedApplicationEvent event) {
        log.info("Admin changed order {} to {}", event.getOrderId(), event.getNewStatus());
        try {
            if (event.getNewStatus() == OrderStatus.CONFIRMED) {
                inventoryService.commitReservation(event.getOrderId());
            } else if (event.getNewStatus() == OrderStatus.CANCELLED) {
                inventoryService.releaseReservation(event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to update stock for order {}", event.getOrderId(), e);
        }

        if (event.getNewStatus() == OrderStatus.CONFIRMED) {
            emailNotificationService.sendOrderConfirmation(event.getOrderId(), event.getUserEmail(), null);
        } else if (event.getNewStatus() == OrderStatus.CANCELLED) {
            emailNotificationService.sendOrderCancellation(
                    event.getOrderId(), event.getUserEmail(), "Cancelled by admin");
        }
    }
}
