package com.shopflow.order.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/** A paid order was cancelled — the customer's payment should be refunded (handled after commit). */
public class OrderRefundApplicationEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String paymentIntentId;

    public OrderRefundApplicationEvent(Object source, UUID orderId, String paymentIntentId) {
        super(source);
        this.orderId = orderId;
        this.paymentIntentId = paymentIntentId;
    }

    public UUID getOrderId() { return orderId; }
    public String getPaymentIntentId() { return paymentIntentId; }
}
