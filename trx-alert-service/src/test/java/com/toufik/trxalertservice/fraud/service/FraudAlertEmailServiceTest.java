package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.config.EmailConfig;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudAlertEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailConfig emailConfig;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private FraudAlertEmailService fraudAlertEmailService;

    private List<FraudAlert> testAlerts;

    @BeforeEach
    void setUp() {
        testAlerts = Arrays.asList(
                new FraudAlert("TXN001", "HIGH_RISK_COUNTRY", "High risk country detected", "HIGH", LocalDateTime.now(), "Details1"),
                new FraudAlert("TXN002", "HIGH_AMOUNT", "High amount detected", "HIGH", LocalDateTime.now(), "Details2")
        );

        when(emailConfig.getSenderEmail()).thenReturn("sender@test.com");
        when(emailConfig.getSenderName()).thenReturn("Fraud System");
        when(emailConfig.getRecipientEmail()).thenReturn("recipient@test.com");
        when(emailConfig.getSubject()).thenReturn("Fraud Alert");
    }

    @Test
    void shouldSendHtmlEmailSuccessfully() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("fraud-alert-email"), any(Context.class)))
                .thenReturn("<html><body>Fraud Alert</body></html>");

        // When
        fraudAlertEmailService.sendFraudAlertEmail(testAlerts);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("fraud-alert-email"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldSetCorrectEmailProperties() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body>Test</body></html>");

        // When
        fraudAlertEmailService.sendFraudAlertEmail(testAlerts);

        // Then
        verify(emailConfig).getSenderEmail();
        verify(emailConfig).getSenderName();
        verify(emailConfig, times(2)).getRecipientEmail(); // Called twice: once in sendHtmlEmail and once in log
        verify(emailConfig).getSubject();
    }

    @Test
    void shouldProcessTemplateWithCorrectContext() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body>Test</body></html>");

        // When
        fraudAlertEmailService.sendFraudAlertEmail(testAlerts);

        // Then
        verify(templateEngine).process(eq("fraud-alert-email"), argThat(context -> {
            Context ctx = (Context) context;
            return ctx.getVariable("alerts").equals(testAlerts) &&
                    ctx.getVariable("alertCount").equals(2);
        }));
    }

    @Test
    void shouldHandleEmptyAlertsList() throws Exception {
        // Given
        List<FraudAlert> emptyAlerts = Arrays.asList();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body>No alerts</body></html>");

        // When
        fraudAlertEmailService.sendFraudAlertEmail(emptyAlerts);

        // Then
        verify(mailSender).send(mimeMessage);
    }
}