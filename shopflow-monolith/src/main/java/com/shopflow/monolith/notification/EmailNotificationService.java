package com.shopflow.monolith.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendOrderConfirmation(UUID orderId, String toEmail, BigDecimal totalAmount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Order Confirmed — ShopFlow #" + orderId.toString().substring(0, 8).toUpperCase());
            message.setText(
                    "Hi,\n\n" +
                    "Your order has been confirmed!\n\n" +
                    "Order ID : " + orderId + "\n" +
                    "Total    : $" + totalAmount + "\n\n" +
                    "Thank you for shopping with ShopFlow.\n\n" +
                    "— The ShopFlow Team"
            );
            mailSender.send(message);
            log.info("Confirmation email sent to {} for order {}", toEmail, orderId);
        } catch (Exception e) {
            log.error("Failed to send confirmation email for order {}: {}", orderId, e.getMessage());
        }
    }

    @Async
    public void sendOrderCancellation(UUID orderId, String toEmail, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Order Cancelled — ShopFlow #" + orderId.toString().substring(0, 8).toUpperCase());
            message.setText(
                    "Hi,\n\n" +
                    "Unfortunately your order could not be fulfilled.\n\n" +
                    "Order ID : " + orderId + "\n" +
                    "Reason   : " + reason + "\n\n" +
                    "Please try again or contact support.\n\n" +
                    "— The ShopFlow Team"
            );
            mailSender.send(message);
            log.info("Cancellation email sent to {} for order {}", toEmail, orderId);
        } catch (Exception e) {
            log.error("Failed to send cancellation email for order {}: {}", orderId, e.getMessage());
        }
    }
}
