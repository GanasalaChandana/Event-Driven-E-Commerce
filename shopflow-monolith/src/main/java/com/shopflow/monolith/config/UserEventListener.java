package com.shopflow.monolith.config;

import com.shopflow.monolith.notification.EmailNotificationService;
import com.shopflow.user.event.UserRegisteredApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailNotificationService emailNotificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredApplicationEvent event) {
        log.info("Sending welcome email to {}", event.getUserEmail());
        emailNotificationService.sendWelcome(event.getUserEmail(), event.getUserName());
    }
}
