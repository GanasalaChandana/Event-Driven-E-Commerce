package com.shopflow.order.dto;

/** url is null when payments are not enabled (the order is then confirmed automatically). */
public record CheckoutResponse(String url) {}
