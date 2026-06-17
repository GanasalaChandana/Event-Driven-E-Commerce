package com.shopflow.order.controller;

import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.service.OrderService;
import com.shopflow.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication auth) {
        var user = resolveUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(user.getId(), user.getEmail(), request));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> myOrders(
            Authentication auth,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(resolveUser(auth).getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> myOrder(
            @PathVariable UUID id,
            Authentication auth) {
        return ResponseEntity.ok(orderService.getMyOrder(id, resolveUser(auth).getId()));
    }

    private com.shopflow.user.entity.User resolveUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "User not found: " + auth.getName()));
    }
}
