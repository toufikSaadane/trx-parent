package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.config.EmailConfig;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudAlertEmailService {

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendFraudAlertEmail(List<FraudAlert> alerts) throws MessagingException, UnsupportedEncodingException {
            sendHtmlEmail(alerts);
            log.info("HTML fraud alert email sent successfully to {}", emailConfig.getRecipientEmail());
    }

    private void sendHtmlEmail(List<FraudAlert> alerts) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(emailConfig.getSenderEmail(), emailConfig.getSenderName());
        helper.setTo(emailConfig.getRecipientEmail());
        helper.setSubject(emailConfig.getSubject());

        // Create Thymeleaf context
        Context context = new Context();
        context.setVariable("alerts", alerts);
        context.setVariable("alertCount", alerts.size());
        context.setVariable("timestamp", java.time.LocalDateTime.now().format(FORMATTER));

        // Process HTML template
        String htmlContent = templateEngine.process("fraud-alert-email", context);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
//
//    private void sendPlainTextEmail(List<FraudAlert> alerts) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom(emailConfig.getSenderEmail());
//        message.setTo(emailConfig.getRecipientEmail());
//        message.setSubject(emailConfig.getSubject());
//
//        StringBuilder content = new StringBuilder();
//        content.append("FRAUD ALERT NOTIFICATION\n");
//        content.append("========================\n\n");
//        content.append("Number of alerts: ").append(alerts.size()).append("\n\n");
//
//        for (int i = 0; i < alerts.size(); i++) {
//            FraudAlert alert = alerts.get(i);
//            content.append("ALERT #").append(i + 1).append("\n");
//            content.append("Transaction ID: ").append(alert.getTransactionId()).append("\n");
//            content.append("Rule: ").append(alert.getRuleName()).append("\n");
//            content.append("Severity: ").append(alert.getSeverity()).append("\n");
//            content.append("Description: ").append(alert.getDescription()).append("\n");
//            content.append("Alert Time: ").append(alert.getAlertTime().format(FORMATTER)).append("\n");
//            content.append("Details: ").append(alert.getDetails()).append("\n");
//            content.append("\n");
//        }
//
//        content.append("Please investigate these transactions immediately.\n\n");
//        content.append("This is an automated message from the Transaction Fraud Detection System.");
//
//        message.setText(content.toString());
//        mailSender.send(message);
//    }
//
//    public void sendTestEmail() {
//        if (!emailConfig.isEnabled()) {
//            log.info("Email notifications are disabled");
//            return;
//        }
//
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom(emailConfig.getSenderEmail());
//            message.setTo(emailConfig.getRecipientEmail());
//            message.setSubject("Test Email - Fraud Detection System");
//            message.setText("This is a test email from the Transaction Fraud Detection System.\n\n" +
//                    "If you receive this email, the email configuration is working correctly.\n\n" +
//                    "Timestamp: " + java.time.LocalDateTime.now().format(FORMATTER));
//
//            mailSender.send(message);
//            log.info("Test email sent successfully to {}", emailConfig.getRecipientEmail());
//        } catch (Exception e) {
//            log.error("Failed to send test email: {}", e.getMessage(), e);
//        }
//    }
}