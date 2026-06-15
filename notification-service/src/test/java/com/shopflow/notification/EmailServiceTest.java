package com.shopflow.notification;

import com.shopflow.notification.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailService emailService;

    @Test
    void sendOrderConfirmed_callsMailSender() throws Exception {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@shopflow.com");
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOrderConfirmed("user@example.com", "ORDER-123", "99.99");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendWelcome_callsMailSender() throws Exception {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@shopflow.com");
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendWelcome("user@example.com", "Chandana");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_doesNotThrowWhenMailFails() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@shopflow.com");
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP down"));

        // Should swallow the exception, not propagate it
        emailService.sendOrderCancelled("user@example.com", "ORDER-456", "Out of stock");
    }
}
