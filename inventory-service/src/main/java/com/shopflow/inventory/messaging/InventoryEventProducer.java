package com.shopflow.inventory.messaging;

import com.shopflow.inventory.event.InventoryFailedEvent;
import com.shopflow.inventory.event.InventoryReservedEvent;
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
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inventory-reserved}")
    private String reservedTopic;

    @Value("${kafka.topics.inventory-failed}")
    private String failedTopic;

    public void publishInventoryReserved(InventoryReservedEvent event) {
        sendAfterCommit(reservedTopic, event.getOrderId(), event);
    }

    public void publishInventoryFailed(InventoryFailedEvent event) {
        sendAfterCommit(failedTopic, event.getOrderId(), event);
    }

    // Only announce a reservation once it is actually committed to the database
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
