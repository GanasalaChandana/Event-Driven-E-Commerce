package com.shopflow.monolith.config;

import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.service.InventoryService;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.event.OrderCancelledApplicationEvent;
import com.shopflow.order.event.OrderPlacedApplicationEvent;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrderFulfillmentService {

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final EmailNotificationService emailNotificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedApplicationEvent event) {
        UUID orderId = event.getOrderId();
        OrderRequest request = event.getRequest();
        log.info("Sync fulfillment triggered for order {}", orderId);

        List<OrderCreatedEvent.OrderItemEvent> items = request.getItems().stream()
                .map(i -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        try {
            boolean reserved = inventoryService.reserveStockSync(orderId, items);
            if (reserved) {
                orderService.confirmOrder(orderId);
                log.info("Order {} CONFIRMED via sync fulfillment", orderId);
                emailNotificationService.sendOrderConfirmation(
                        orderId, event.getUserEmail(), event.getTotalAmount());
            } else {
                orderService.cancelOrder(orderId, "Insufficient stock");
                log.warn("Order {} CANCELLED via sync fulfillment — insufficient stock", orderId);
                emailNotificationService.sendOrderCancellation(
                        orderId, event.getUserEmail(), "Insufficient stock");
            }
        } catch (Exception e) {
            log.error("Sync fulfillment failed for order {}: {}", orderId, e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelledByUser(OrderCancelledApplicationEvent event) {
        log.info("Sending cancellation email for order {} cancelled by customer", event.getOrderId());
        emailNotificationService.sendOrderCancellation(
                event.getOrderId(), event.getUserEmail(), "Cancelled by customer");
    }
}
