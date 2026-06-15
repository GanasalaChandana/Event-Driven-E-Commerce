package com.shopflow.notification.messaging;

import com.shopflow.notification.event.OrderCancelledEvent;
import com.shopflow.notification.event.OrderConfirmedEvent;
import com.shopflow.notification.event.UserRegisteredEvent;
import com.shopflow.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final EmailService emailService;

    @KafkaListener(
        topics = "${kafka.topics.order-confirmed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Sending order confirmation email to {}", event.getUserEmail());
        emailService.sendOrderConfirmed(
                event.getUserEmail(),
                event.getOrderId().toString(),
                event.getTotalAmount().toPlainString()
        );
    }

    @KafkaListener(
        topics = "${kafka.topics.order-cancelled}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Sending order cancellation email to {}", event.getUserEmail());
        emailService.sendOrderCancelled(
                event.getUserEmail(),
                event.getOrderId().toString(),
                event.getReason()
        );
    }

    @KafkaListener(
        topics = "${kafka.topics.user-registered}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Sending welcome email to {}", event.getEmail());
        emailService.sendWelcome(event.getEmail(), event.getName());
    }
}
