package com.shopflow.monolith.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final RestTemplate restTemplate;

    @Value("${resend.api-key}")
    private String apiKey;

    private static final String RESEND_URL = "https://api.resend.com/emails";
    private static final String FROM = "ShopFlow <onboarding@resend.dev>";

    @Async
    public void sendOrderConfirmation(UUID orderId, String toEmail, BigDecimal totalAmount) {
        String subject = "Order Confirmed — ShopFlow #" + orderId.toString().substring(0, 8).toUpperCase();
        String text = "Hi,\n\n" +
                "Your order has been confirmed!\n\n" +
                "Order ID : " + orderId + "\n" +
                "Total    : $" + totalAmount + "\n\n" +
                "Thank you for shopping with ShopFlow.\n\n" +
                "— The ShopFlow Team";
        send(toEmail, subject, text, orderId);
    }

    @Async
    public void sendOrderCancellation(UUID orderId, String toEmail, String reason) {
        String subject = "Order Cancelled — ShopFlow #" + orderId.toString().substring(0, 8).toUpperCase();
        String text = "Hi,\n\n" +
                "Unfortunately your order could not be fulfilled.\n\n" +
                "Order ID : " + orderId + "\n" +
                "Reason   : " + reason + "\n\n" +
                "Please try again or contact support.\n\n" +
                "— The ShopFlow Team";
        send(toEmail, subject, text, orderId);
    }

    private void send(String toEmail, String subject, String text, UUID orderId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "from", FROM,
                    "to", List.of(toEmail),
                    "subject", subject,
                    "text", text
            );

            restTemplate.postForObject(RESEND_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Email sent to {} for order {}", toEmail, orderId);
        } catch (Exception e) {
            log.error("Failed to send email for order {}: {}", orderId, e.getMessage());
        }
    }
}
