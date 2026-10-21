package com.shopflow.order.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderPlacedApplicationEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String userEmail;
    private final BigDecimal totalAmount;
    // Server-priced line items as persisted (never the raw client request)
    private final List<OrderCreatedEvent.OrderItemEvent> items;

    public OrderPlacedApplicationEvent(Object source, UUID orderId, String userEmail,
                                       BigDecimal totalAmount, List<OrderCreatedEvent.OrderItemEvent> items) {
        super(source);
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
        this.items = List.copyOf(items);
    }

    public UUID getOrderId() { return orderId; }
    public String getUserEmail() { return userEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderCreatedEvent.OrderItemEvent> getItems() { return items; }
}
