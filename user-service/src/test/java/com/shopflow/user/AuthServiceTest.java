package com.shopflow.user;

import com.shopflow.user.dto.RegisterRequest;
import com.shopflow.user.entity.User;
import com.shopflow.user.event.UserRegisteredEvent;
import com.shopflow.user.exception.EmailAlreadyExistsException;
import com.shopflow.user.repository.UserRepository;
import com.shopflow.user.service.AuthService;
import com.shopflow.user.service.JwtService;
import com.shopflow.user.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsServiceImpl userDetailsService;
    @Mock KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @InjectMocks AuthService authService;

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test");
        req.setEmail("test@example.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any(User.class));
    }
}
