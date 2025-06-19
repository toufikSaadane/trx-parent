package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.model.FraudAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FraudAlertNotificationService {

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

            // Here you would integrate with:
            // - Email service
            // - SMS service
            // - Slack/Teams notifications
            // - Database persistence
            // - External fraud monitoring systems
        }

        log.error("============================================================");
    }
}