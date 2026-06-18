package com.shopflow.order.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class OrderCancelledApplicationEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String userEmail;

    public OrderCancelledApplicationEvent(Object source, UUID orderId, String userEmail) {
        super(source);
        this.orderId = orderId;
        this.userEmail = userEmail;
    }

    public UUID getOrderId() { return orderId; }
    public String getUserEmail() { return userEmail; }
}
