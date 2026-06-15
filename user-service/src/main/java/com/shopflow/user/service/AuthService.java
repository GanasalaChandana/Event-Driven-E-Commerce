package com.shopflow.user.service;

import com.shopflow.user.dto.AuthResponse;
import com.shopflow.user.dto.LoginRequest;
import com.shopflow.user.dto.RegisterRequest;
import com.shopflow.user.dto.UserResponse;
import com.shopflow.user.entity.User;
import com.shopflow.user.event.UserRegisteredEvent;
import com.shopflow.user.exception.EmailAlreadyExistsException;
import com.shopflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @Value("${kafka.topics.user-registered:user.registered}")
    private String userRegisteredTopic;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        // Publish event so Notification Service can send a welcome email
        kafkaTemplate.send(userRegisteredTopic, user.getId().toString(),
                UserRegisteredEvent.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .registeredAt(LocalDateTime.now())
                        .build());

        String token = generateToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = generateToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }

    private String generateToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return jwtService.generateToken(userDetails, Map.of(
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "name", user.getName()
        ));
    }
}
