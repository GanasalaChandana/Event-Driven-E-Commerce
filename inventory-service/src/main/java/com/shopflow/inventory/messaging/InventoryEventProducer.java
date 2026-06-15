package com.shopflow.inventory.messaging;

import com.shopflow.inventory.event.InventoryFailedEvent;
import com.shopflow.inventory.event.InventoryReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
        kafkaTemplate.send(reservedTopic, event.getOrderId().toString(), event);
        log.info("Published inventory.reserved for order {}", event.getOrderId());
    }

    public void publishInventoryFailed(InventoryFailedEvent event) {
        kafkaTemplate.send(failedTopic, event.getOrderId().toString(), event);
        log.info("Published inventory.failed for order {}", event.getOrderId());
    }
}
