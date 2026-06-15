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

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // X-User-Id and X-User-Email are injected by the API Gateway after JWT validation
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userId, userEmail, request));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> myOrders(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> myOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(orderService.getMyOrder(id, userId));
    }
}
