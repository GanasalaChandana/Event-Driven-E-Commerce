package com.shopflow.user.config;

import com.shopflow.user.entity.User;
import com.shopflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Creates (or re-syncs) the admin account from environment variables.
 *
 * There is deliberately no default password: if ADMIN_PASSWORD is missing or too
 * short, the bootstrap is skipped and any existing admin account is left untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@shopflow.com}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD is not set — skipping admin bootstrap (existing admin account unchanged)");
            return;
        }
        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD is shorter than {} characters — skipping admin bootstrap", MIN_PASSWORD_LENGTH);
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                admin -> {
                    boolean changed = false;
                    if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                        admin.setPassword(passwordEncoder.encode(adminPassword));
                        changed = true;
                    }
                    if (admin.getRole() != User.Role.ADMIN) {
                        admin.setRole(User.Role.ADMIN);
                        changed = true;
                    }
                    if (changed) {
                        userRepository.save(admin);
                        log.info("Admin account {} synced from environment", email);
                    }
                },
                () -> {
                    userRepository.save(User.builder()
                            .name("Admin")
                            .email(email)
                            .password(passwordEncoder.encode(adminPassword))
                            .role(User.Role.ADMIN)
                            .build());
                    log.info("Admin account created: {}", email);
                }
        );
    }
}
