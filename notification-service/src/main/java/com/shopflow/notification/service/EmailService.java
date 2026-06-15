package com.shopflow.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    @Async
    public void sendOrderConfirmed(String to, String orderId, String totalAmount) {
        String subject = "Your ShopFlow order is confirmed!";
        String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>"
                + "<h2 style='color:#16a34a'>Order Confirmed</h2>"
                + "<p>Your order has been confirmed and is being prepared.</p>"
                + "<table style='width:100%;border-collapse:collapse'>"
                + "<tr><td style='padding:8px;font-weight:bold'>Order ID</td>"
                + "<td style='padding:8px'>" + orderId + "</td></tr>"
                + "<tr style='background:#f9fafb'><td style='padding:8px;font-weight:bold'>Total</td>"
                + "<td style='padding:8px'>$" + totalAmount + "</td></tr>"
                + "</table>"
                + "<p style='margin-top:24px;color:#6b7280'>Thank you for shopping with ShopFlow.</p>"
                + "</div>";
        send(to, subject, body);
    }

    @Async
    public void sendOrderCancelled(String to, String orderId, String reason) {
        String subject = "Your ShopFlow order was cancelled";
        String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>"
                + "<h2 style='color:#dc2626'>Order Cancelled</h2>"
                + "<p>Unfortunately your order could not be processed.</p>"
                + "<table style='width:100%;border-collapse:collapse'>"
                + "<tr><td style='padding:8px;font-weight:bold'>Order ID</td>"
                + "<td style='padding:8px'>" + orderId + "</td></tr>"
                + "<tr style='background:#f9fafb'><td style='padding:8px;font-weight:bold'>Reason</td>"
                + "<td style='padding:8px'>" + reason + "</td></tr>"
                + "</table>"
                + "<p style='margin-top:24px;color:#6b7280'>No payment has been taken.</p>"
                + "</div>";
        send(to, subject, body);
    }

    @Async
    public void sendWelcome(String to, String name) {
        String subject = "Welcome to ShopFlow!";
        String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>"
                + "<h2 style='color:#2563eb'>Welcome, " + name + "!</h2>"
                + "<p>Your account has been created successfully.</p>"
                + "<p>You can now browse products, place orders, and track deliveries.</p>"
                + "<p style='margin-top:32px;color:#6b7280;font-size:12px'>"
                + "If you did not create this account, please ignore this email.</p>"
                + "</div>";
        send(to, subject, body);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} — {}", to, subject);
        } catch (Exception e) {
            // Log but never crash — email failure must not block order processing
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
