package com.shopflow.order.controller;

import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Caller identity comes from the X-User-* headers. They are only ever derived from a
 * verified JWT: by the API Gateway (which strips any client-supplied copies) in the
 * microservices deployment, or by GatewayHeaderFilter in the monolith.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    static final String USER_ID = "X-User-Id";
    static final String USER_EMAIL = "X-User-Email";

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(USER_ID) UUID userId,
            @RequestHeader(USER_EMAIL) String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userId, userEmail, request));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> myOrders(
            @RequestHeader(USER_ID) UUID userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> myOrder(
            @PathVariable UUID id,
            @RequestHeader(USER_ID) UUID userId) {
        return ResponseEntity.ok(orderService.getMyOrder(id, userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @RequestHeader(USER_ID) UUID userId) {
        return ResponseEntity.ok(orderService.cancelMyOrder(id, userId));
    }
}
