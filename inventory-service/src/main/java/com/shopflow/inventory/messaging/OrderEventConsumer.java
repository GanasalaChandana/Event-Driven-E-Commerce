package com.shopflow.inventory.messaging;

import com.shopflow.inventory.event.OrderCreatedEvent;
import com.shopflow.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
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
}
