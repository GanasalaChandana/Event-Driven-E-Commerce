package com.shopflow.order.service;

import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.payment.PaymentProperties;
import com.shopflow.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/** Cancels orders that were never paid so their reserved stock goes back on sale. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidOrderCleanup {

    private final PaymentProperties props;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void cancelExpiredOrders() {
        if (!props.enabled()) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(props.getUnpaidTimeoutMinutes());
        for (Order expired : orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff)) {
            UUID id = expired.getId();
            try {
                if (orderService.cancelUnpaidOrder(id)) {
                    log.info("Order {} cancelled — not paid within {} minutes", id, props.getUnpaidTimeoutMinutes());
                }
            } catch (Exception e) {
                log.error("Could not cancel unpaid order {}", id, e);
            }
        }
    }
}
