package com.shopflow.inventory.messaging;

import com.shopflow.inventory.event.InventoryReleaseEvent;
import com.shopflow.inventory.event.OrderConfirmedEvent;
import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// The monolith fulfils orders in-process and disables these listeners (shopflow.kafka.listeners-enabled=false)
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "shopflow.kafka.listeners-enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "${kafka.topics.order-created}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created for order {} with {} items",
                event.getOrderId(), event.getItems().size());
        inventoryService.processOrderCreated(event);
    }

    @KafkaListener(topics = "${kafka.topics.order-confirmed}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received order.confirmed for order {}", event.getOrderId());
        inventoryService.commitReservation(event.getOrderId());
    }

    @KafkaListener(topics = "${kafka.topics.inventory-release}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryRelease(InventoryReleaseEvent event) {
        log.info("Received inventory.release for order {} — {}", event.getOrderId(), event.getReason());
        inventoryService.releaseReservation(event.getOrderId());
    }
}
