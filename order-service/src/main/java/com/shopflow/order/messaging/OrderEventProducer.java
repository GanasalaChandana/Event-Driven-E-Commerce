package com.shopflow.order.messaging;

import com.shopflow.order.event.OrderCancelledEvent;
import com.shopflow.order.event.OrderConfirmedEvent;
import com.shopflow.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(orderCreatedTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.warn("Kafka unavailable, skipping order.created: {}", ex.getMessage());
                    else log.info("Published order.created for order {}", event.getOrderId());
                });
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        kafkaTemplate.send(orderConfirmedTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.warn("Kafka unavailable, skipping order.confirmed: {}", ex.getMessage());
                    else log.info("Published order.confirmed for order {}", event.getOrderId());
                });
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(orderCancelledTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.warn("Kafka unavailable, skipping order.cancelled: {}", ex.getMessage());
                    else log.info("Published order.cancelled for order {}", event.getOrderId());
                });
    }
}
