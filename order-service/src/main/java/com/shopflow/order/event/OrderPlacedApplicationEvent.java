package com.shopflow.order.event;

import com.shopflow.order.dto.OrderRequest;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class OrderPlacedApplicationEvent extends ApplicationEvent {

    private final UUID orderId;
    private final OrderRequest request;

    public OrderPlacedApplicationEvent(Object source, UUID orderId, OrderRequest request) {
        super(source);
        this.orderId = orderId;
        this.request = request;
    }

    public UUID getOrderId() { return orderId; }
    public OrderRequest getRequest() { return request; }
}
