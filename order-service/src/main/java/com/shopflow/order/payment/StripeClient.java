package com.shopflow.order.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.dto.OrderItemResponse;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Minimal Stripe REST client (Checkout Sessions + refunds) — no SDK needed. */
@Slf4j
@Component
public class StripeClient {

    private static final String API = "https://api.stripe.com/v1";
    /** Stripe requires a Checkout Session to stay open for at least 30 minutes. */
    private static final int SESSION_MINUTES = 31;

    private final PaymentProperties props;
    private final ObjectMapper mapper;
    private final RestClient http = RestClient.builder().baseUrl(API).build();

    public StripeClient(PaymentProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    /** Creates a hosted Checkout page for the order and returns its URL. */
    public String createCheckoutSession(OrderResponse order) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("client_reference_id", order.getId().toString());
        form.add("metadata[orderId]", order.getId().toString());
        form.add("customer_email", order.getUserEmail());
        form.add("success_url", props.getFrontendUrl() + "/orders/success?order=" + order.getId());
        form.add("cancel_url", props.getFrontendUrl() + "/orders?payment=cancelled");
        form.add("expires_at", String.valueOf(Instant.now().plus(SESSION_MINUTES, ChronoUnit.MINUTES).getEpochSecond()));

        int i = 0;
        for (OrderItemResponse item : order.getItems()) {
            String p = "line_items[" + i++ + "]";
            form.add(p + "[quantity]", String.valueOf(item.getQuantity()));
            form.add(p + "[price_data][currency]", "usd");
            form.add(p + "[price_data][unit_amount]", String.valueOf(toCents(item.getUnitPrice())));
            form.add(p + "[price_data][product_data][name]", item.getProductName());
        }

        JsonNode session = post("/checkout/sessions", form, null);
        String url = session.path("url").asText(null);
        if (url == null || url.isBlank()) {
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "Payment provider returned no checkout URL");
        }
        return url;
    }

    /** Refunds the full amount of a payment. Throws if Stripe rejects it so the caller can retry. */
    public void refund(String paymentIntentId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("payment_intent", paymentIntentId);
        post("/refunds", form, "refund-" + paymentIntentId);
        log.info("Refunded payment {}", paymentIntentId);
    }

    public static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private JsonNode post(String path, MultiValueMap<String, String> form, String idempotencyKey) {
        try {
            RestClient.RequestBodySpec request = http.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getSecretKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (idempotencyKey != null) {
                request = request.header("Idempotency-Key", idempotencyKey);
            }
            String body = request.body(form).retrieve().body(String.class);
            return mapper.readTree(body);
        } catch (RestClientResponseException e) {
            log.error("Stripe {} failed: HTTP {} {}", path, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "Payment provider error", e);
        } catch (RestClientException | JsonProcessingException e) {
            log.error("Stripe {} failed", path, e);
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "Payment provider unavailable", e);
        }
    }
}
