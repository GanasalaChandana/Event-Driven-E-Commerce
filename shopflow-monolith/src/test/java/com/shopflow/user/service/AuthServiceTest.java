package com.shopflow.user.service;

import com.shopflow.user.dto.AuthResponse;
import com.shopflow.user.dto.LoginRequest;
import com.shopflow.user.dto.RegisterRequest;
import com.shopflow.user.entity.User;
import com.shopflow.user.event.UserRegisteredEvent;
import com.shopflow.user.exception.EmailAlreadyExistsException;
import com.shopflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;
    @Mock KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@example.com")
                .password("hashed")
                .role(User.Role.USER)
                .build();
    }

    @Test
    void register_happyPath_returnsTokenAndUser() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername("alice@example.com").password("hashed").roles("USER").build();
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(ud);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername("alice@example.com").password("hashed").roles("USER").build();
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(ud);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("alice@example.com", "password123"));
    }

    @Test
    void login_badPassword_throwsBadCredentialsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }
}
