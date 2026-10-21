package com.shopflow.order.controller;

import com.shopflow.order.dto.CheckoutResponse;
import com.shopflow.order.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Starts payment for the caller's own PENDING order. url is null when payments are disabled. */
    @PostMapping("/api/v1/orders/{id}/checkout")
    public CheckoutResponse checkout(@PathVariable UUID id,
                                     @RequestHeader(OrderController.USER_ID) UUID userId) {
        return paymentService.startCheckout(id, userId);
    }

    /** Called by Stripe. Public, but every request must carry a valid Stripe-Signature. */
    @PostMapping("/api/v1/payments/webhook")
    public ResponseEntity<Void> webhook(@RequestBody byte[] payload,
                                        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
