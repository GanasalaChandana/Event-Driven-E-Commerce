package com.shopflow.order.event;

import com.shopflow.order.entity.OrderStatus;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class OrderStatusChangedApplicationEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String userEmail;
    private final OrderStatus newStatus;

    public OrderStatusChangedApplicationEvent(Object source, UUID orderId, String userEmail, OrderStatus newStatus) {
        super(source);
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.newStatus = newStatus;
    }

    public UUID getOrderId() { return orderId; }
    public String getUserEmail() { return userEmail; }
    public OrderStatus getNewStatus() { return newStatus; }
}
