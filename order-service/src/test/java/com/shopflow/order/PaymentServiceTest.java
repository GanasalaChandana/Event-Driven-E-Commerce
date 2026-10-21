package com.shopflow.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.dto.CheckoutResponse;
import com.shopflow.order.dto.OrderItemResponse;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.exception.PaymentException;
import com.shopflow.order.payment.PaymentProperties;
import com.shopflow.order.payment.PaymentService;
import com.shopflow.order.payment.StripeClient;
import com.shopflow.order.payment.StripeSignature;
import com.shopflow.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String SECRET = "whsec_test";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock PaymentProperties props;
    @Mock StripeClient stripe;
    @Mock OrderService orderService;

    PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(props, stripe, orderService, new ObjectMapper());
    }

    private static OrderResponse order(String status, String total) {
        return OrderResponse.builder()
                .id(ORDER_ID).userId(USER_ID).userEmail("u@test.com").status(status)
                .totalAmount(new BigDecimal(total))
                .items(List.of(OrderItemResponse.builder().productId(UUID.randomUUID())
                        .productName("Wireless Headphones").quantity(1).unitPrice(new BigDecimal(total)).build()))
                .build();
    }

    private void paymentsOn() {
        when(props.enabled()).thenReturn(true);
    }

    private void webhookSecret() {
        when(props.getWebhookSecret()).thenReturn(SECRET);
    }

    private static String sessionEvent(String type, String paymentStatus, long amountCents, String reference) {
        return "{\"type\":\"" + type + "\",\"data\":{\"object\":{\"payment_status\":\"" + paymentStatus
                + "\",\"client_reference_id\":\"" + reference + "\",\"payment_intent\":\"pi_123\",\"amount_total\":"
                + amountCents + "}}}";
    }

    private void deliver(String json) {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        long t = Instant.now().getEpochSecond();
        service.handleWebhook(payload, "t=" + t + ",v1=" + StripeSignature.hmac(t, payload, SECRET));
    }

    // ── checkout ────────────────────────────────────────────────────────────

    @Test
    void startCheckout_paymentsDisabled_returnsNoUrl() {
        when(props.enabled()).thenReturn(false);

        CheckoutResponse response = service.startCheckout(ORDER_ID, USER_ID);

        assertThat(response.url()).isNull();
        verifyNoInteractions(stripe);
    }

    @Test
    void startCheckout_pendingOrder_returnsStripeUrl() {
        paymentsOn();
        OrderResponse o = order("PENDING", "99.99");
        when(orderService.getMyOrder(ORDER_ID, USER_ID)).thenReturn(o);
        when(stripe.createCheckoutSession(o)).thenReturn("https://checkout.stripe.com/pay/cs_test");

        assertThat(service.startCheckout(ORDER_ID, USER_ID).url()).isEqualTo("https://checkout.stripe.com/pay/cs_test");
    }

    @Test
    void startCheckout_cancelledOrder_isConflict() {
        paymentsOn();
        when(orderService.getMyOrder(ORDER_ID, USER_ID)).thenReturn(order("CANCELLED", "99.99"));

        assertThatThrownBy(() -> service.startCheckout(ORDER_ID, USER_ID))
                .isInstanceOfSatisfying(PaymentException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(stripe);
    }

    // ── webhook ─────────────────────────────────────────────────────────────

    @Test
    void webhook_invalidSignature_isRejected() {
        paymentsOn();
        webhookSecret();

        assertThatThrownBy(() -> service.handleWebhook("{}".getBytes(StandardCharsets.UTF_8), "t=1,v1=bad"))
                .isInstanceOfSatisfying(PaymentException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(orderService);
    }

    @Test
    void webhook_paymentsDisabled_isNotFound() {
        when(props.enabled()).thenReturn(false);

        assertThatThrownBy(() -> service.handleWebhook(new byte[0], null))
                .isInstanceOfSatisfying(PaymentException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void webhook_paidSession_confirmsOrder() {
        paymentsOn();
        webhookSecret();
        when(orderService.getOrderById(ORDER_ID)).thenReturn(order("PENDING", "99.99"));
        when(orderService.confirmPaidOrder(ORDER_ID)).thenReturn(true);

        deliver(sessionEvent("checkout.session.completed", "paid", 9999, ORDER_ID.toString()));

        verify(orderService).confirmPaidOrder(ORDER_ID);
        verify(stripe, never()).refund(any());
    }

    @Test
    void webhook_duplicateDelivery_forConfirmedOrder_isIgnored() {
        paymentsOn();
        webhookSecret();
        when(orderService.getOrderById(ORDER_ID)).thenReturn(order("CONFIRMED", "99.99"));

        deliver(sessionEvent("checkout.session.completed", "paid", 9999, ORDER_ID.toString()));

        verify(orderService, never()).confirmPaidOrder(any());
        verify(stripe, never()).refund(any());
    }

    @Test
    void webhook_paymentForCancelledOrder_isRefunded() {
        paymentsOn();
        webhookSecret();
        when(orderService.getOrderById(ORDER_ID)).thenReturn(order("CANCELLED", "99.99"));
        when(orderService.confirmPaidOrder(ORDER_ID)).thenReturn(false);

        deliver(sessionEvent("checkout.session.completed", "paid", 9999, ORDER_ID.toString()));

        verify(stripe).refund("pi_123");
    }

    @Test
    void webhook_wrongAmount_isRefundedWithoutConfirming() {
        paymentsOn();
        webhookSecret();
        when(orderService.getOrderById(ORDER_ID)).thenReturn(order("PENDING", "99.99"));

        deliver(sessionEvent("checkout.session.completed", "paid", 100, ORDER_ID.toString()));

        verify(orderService, never()).confirmPaidOrder(any());
        verify(stripe).refund("pi_123");
    }

    @Test
    void webhook_unknownOrder_isRefunded() {
        paymentsOn();
        webhookSecret();
        when(orderService.getOrderById(ORDER_ID)).thenThrow(new OrderNotFoundException(ORDER_ID));

        deliver(sessionEvent("checkout.session.completed", "paid", 9999, ORDER_ID.toString()));

        verify(stripe).refund("pi_123");
    }

    @Test
    void webhook_unpaidSessionOrOtherEvent_isIgnored() {
        paymentsOn();
        webhookSecret();

        deliver(sessionEvent("checkout.session.completed", "unpaid", 9999, ORDER_ID.toString()));
        deliver(sessionEvent("payment_intent.created", "paid", 9999, ORDER_ID.toString()));

        verifyNoInteractions(orderService);
        verify(stripe, never()).refund(any());
    }

    @Test
    void toCents_roundsToWholeCents() {
        assertThat(StripeClient.toCents(new BigDecimal("99.99"))).isEqualTo(9999L);
        assertThat(StripeClient.toCents(new BigDecimal("249.9"))).isEqualTo(24990L);
    }
}
