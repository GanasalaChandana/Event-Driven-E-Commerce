package com.shopflow.order.messaging;

import com.shopflow.order.event.InventoryReleaseEvent;
import com.shopflow.order.event.OrderCancelledEvent;
import com.shopflow.order.event.OrderConfirmedEvent;
import com.shopflow.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    @Value("${kafka.topics.inventory-release}")
    private String inventoryReleaseTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        sendAfterCommit(orderCreatedTopic, event.getOrderId(), event);
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        sendAfterCommit(orderConfirmedTopic, event.getOrderId(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        sendAfterCommit(orderCancelledTopic, event.getOrderId(), event);
    }

    public void publishInventoryRelease(InventoryReleaseEvent event) {
        sendAfterCommit(inventoryReleaseTopic, event.getOrderId(), event);
    }

    /**
     * Events describe state that is only real once the DB transaction commits. Sending
     * inside the transaction lets consumers react to an order that doesn't exist yet
     * (or never will, if the transaction rolls back), so defer the send to afterCommit.
     */
    private void sendAfterCommit(String topic, UUID orderId, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(topic, orderId, event);
                }
            });
        } else {
            send(topic, orderId, event);
        }
    }

    private void send(String topic, UUID orderId, Object event) {
        try {
            kafkaTemplate.send(topic, orderId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) log.warn("Kafka unavailable, skipping {} for order {}: {}", topic, orderId, ex.getMessage());
                        else log.info("Published {} for order {}", topic, orderId);
                    });
        } catch (Exception e) {
            log.warn("Kafka send threw synchronously for {} (order {}): {}", topic, orderId, e.getMessage());
        }
    }
}
