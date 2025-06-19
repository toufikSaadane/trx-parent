package com.toufik.trxalertservice.fraud;

import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.service.FraudAlertEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudAlertNotificationService {

    private final FraudAlertEmailService emailService;

    public void sendFraudAlerts(List<FraudAlert> alerts) {
        if (alerts.isEmpty()) {
            return;
        }

        log.error("================= FRAUD ALERT NOTIFICATION =================");

        for (FraudAlert alert : alerts) {
            log.error(" FRAUD ALERT ");
            log.error("  Transaction ID: {}", alert.getTransactionId());
            log.error("  Rule: {}", alert.getRuleName());
            log.error("  Severity: {}", alert.getSeverity());
            log.error("  Description: {}", alert.getDescription());
            log.error("  Alert Time: {}", alert.getAlertTime());
            log.error("  Details: {}", alert.getDetails());
            log.error("------------------------------------------------------------");
        }

        log.error("============================================================");

        // Send email notification
        try {
            emailService.sendFraudAlertEmail(alerts);
            log.info("Email fraud alert sent successfully for {} alert(s)", alerts.size());
        } catch (Exception e) {
            log.error("Failed to send email fraud alert: {}", e.getMessage(), e);
        }

        // Here you can add additional notification channels:
        // - SMS service
        // - Slack/Teams notifications
        // - Database persistence
        // - External fraud monitoring systems
        // - Push notifications
        // - Webhook notifications
    }

    public void sendTestNotification() {
        log.info("Sending test fraud alert notification...");
        emailService.sendTestEmail();
    }
}