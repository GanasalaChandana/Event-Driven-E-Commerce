package com.shopflow.order.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stripe settings. Payment is optional: when the secret key or the webhook secret is not
 * configured, orders are confirmed automatically as before and no checkout is offered.
 */
@Component
public class PaymentProperties {

    private final String secretKey;
    private final String webhookSecret;
    private final String frontendUrl;
    private final int unpaidTimeoutMinutes;

    public PaymentProperties(
            @Value("${payments.stripe.secret-key:}") String secretKey,
            @Value("${payments.stripe.webhook-secret:}") String webhookSecret,
            @Value("${payments.frontend-url:http://localhost:3000}") String frontendUrl,
            @Value("${payments.unpaid-timeout-minutes:35}") int unpaidTimeoutMinutes) {
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.unpaidTimeoutMinutes = unpaidTimeoutMinutes;
    }

    public boolean enabled() {
        return !secretKey.isEmpty() && !webhookSecret.isEmpty();
    }

    public String getSecretKey() { return secretKey; }
    public String getWebhookSecret() { return webhookSecret; }
    public String getFrontendUrl() { return frontendUrl; }
    public int getUnpaidTimeoutMinutes() { return unpaidTimeoutMinutes; }
}
