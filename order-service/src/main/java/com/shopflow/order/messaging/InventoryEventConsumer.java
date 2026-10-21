package com.shopflow.order.messaging;

import com.shopflow.order.event.InventoryFailedEvent;
import com.shopflow.order.event.InventoryReservedEvent;
import com.shopflow.order.service.OrderService;
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
public class InventoryEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${kafka.topics.inventory-reserved}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Received inventory.reserved for order {}", event.getOrderId());
        orderService.handleInventoryReserved(event.getOrderId());
    }

    @KafkaListener(topics = "${kafka.topics.inventory-failed}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryFailed(InventoryFailedEvent event) {
        log.info("Received inventory.failed for order {} — {}", event.getOrderId(), event.getReason());
        orderService.cancelOrder(event.getOrderId(), event.getReason());
    }
}
