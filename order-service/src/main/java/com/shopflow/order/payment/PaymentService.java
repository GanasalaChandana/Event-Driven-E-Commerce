package com.shopflow.order.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.dto.CheckoutResponse;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.exception.PaymentException;
import com.shopflow.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Stripe Checkout flow. The order stays PENDING (stock reserved) until the webhook reports a
 * successful payment; unpaid orders are cancelled by UnpaidOrderCleanup. A payment that arrives
 * for an order that can no longer be confirmed is refunded automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Duration SIGNATURE_TOLERANCE = Duration.ofMinutes(5);

    private final PaymentProperties props;
    private final StripeClient stripe;
    private final OrderService orderService;
    private final ObjectMapper mapper;

    /** Returns the Stripe-hosted payment page for the caller's PENDING order. */
    public CheckoutResponse startCheckout(UUID orderId, UUID userId) {
        if (!props.enabled()) {
            return new CheckoutResponse(null);
        }
        OrderResponse order = orderService.getMyOrder(orderId, userId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new PaymentException(HttpStatus.CONFLICT,
                    "This order is " + order.getStatus() + " and can no longer be paid"
                            + ("CANCELLED".equals(order.getStatus()) ? " (it may have been out of stock)" : ""));
        }
        return new CheckoutResponse(stripe.createCheckoutSession(order));
    }

    public void handleWebhook(byte[] payload, String signatureHeader) {
        if (!props.enabled()) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "Payments are not enabled");
        }
        if (!StripeSignature.verify(payload, signatureHeader, props.getWebhookSecret(), SIGNATURE_TOLERANCE, Instant.now())) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }

        JsonNode event;
        try {
            event = mapper.readTree(payload);
        } catch (java.io.IOException e) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "Malformed event");
        }

        String type = event.path("type").asText();
        if (!type.equals("checkout.session.completed") && !type.equals("checkout.session.async_payment_succeeded")) {
            return; // not interested — acknowledge so Stripe stops retrying
        }
        JsonNode session = event.path("data").path("object");
        if (!"paid".equals(session.path("payment_status").asText())) {
            return;
        }

        String paymentIntent = session.path("payment_intent").asText(null);
        UUID orderId;
        try {
            orderId = UUID.fromString(session.path("client_reference_id").asText(""));
        } catch (IllegalArgumentException e) {
            log.warn("Paid checkout session without a valid order reference — refunding");
            refundIfPossible(paymentIntent);
            return;
        }
        long paidCents = session.path("amount_total").asLong(-1);

        OrderResponse order;
        try {
            order = orderService.getOrderById(orderId);
        } catch (OrderNotFoundException e) {
            log.warn("Payment received for unknown order {} — refunding", orderId);
            refundIfPossible(paymentIntent);
            return;
        }

        if ("CONFIRMED".equals(order.getStatus())) {
            return; // duplicate delivery of the same event
        }
        if (paidCents != StripeClient.toCents(order.getTotalAmount())) {
            log.error("Order {}: paid {} cents but total is {} — refunding", orderId, paidCents, order.getTotalAmount());
            refundIfPossible(paymentIntent);
            return;
        }
        if (orderService.confirmPaidOrder(orderId)) {
            log.info("Order {} paid and CONFIRMED", orderId);
        } else {
            log.warn("Order {} was {} when payment arrived — refunding", orderId, order.getStatus());
            refundIfPossible(paymentIntent);
        }
    }

    /** A failed refund propagates as an error so Stripe redelivers the webhook and we retry. */
    private void refundIfPossible(String paymentIntent) {
        if (paymentIntent == null || paymentIntent.isBlank()) {
            log.error("Cannot refund automatically: no payment_intent on the session — refund it in the Stripe dashboard");
            return;
        }
        stripe.refund(paymentIntent);
    }
}
