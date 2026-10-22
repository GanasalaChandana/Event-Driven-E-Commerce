package com.shopflow.order.payment;

import com.shopflow.order.event.OrderRefundApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Refunds the Stripe payment once the cancellation has been committed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundListener {

    private final StripeClient stripe;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundRequested(OrderRefundApplicationEvent event) {
        try {
            stripe.refund(event.getPaymentIntentId());
            log.info("Order {} cancelled — payment {} refunded", event.getOrderId(), event.getPaymentIntentId());
        } catch (Exception e) {
            // The cancellation is already committed; don't fail the request, but make this impossible to miss
            log.error("REFUND FAILED for cancelled order {} (payment {}) — refund it manually in the Stripe dashboard",
                    event.getOrderId(), event.getPaymentIntentId(), e);
        }
    }
}
