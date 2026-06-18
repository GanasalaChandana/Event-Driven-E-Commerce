package com.shopflow.order.event;

import com.shopflow.order.dto.OrderRequest;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderPlacedApplicationEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String userEmail;
    private final BigDecimal totalAmount;
    private final OrderRequest request;

    public OrderPlacedApplicationEvent(Object source, UUID orderId, String userEmail,
                                       BigDecimal totalAmount, OrderRequest request) {
        super(source);
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
        this.request = request;
    }

    public UUID getOrderId() { return orderId; }
    public String getUserEmail() { return userEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderRequest getRequest() { return request; }
}
