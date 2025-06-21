package com.toufik.trxalertservice.controller;

import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.fraud.FraudAlertNotificationService;
import com.toufik.trxalertservice.fraud.service.FraudAlertEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud-alerts")
@RequiredArgsConstructor
@Slf4j
public class FraudAlertTestController {

    private final FraudAlertNotificationService notificationService;
    private final FraudAlertEmailService emailService;

    @PostMapping("/test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail() {
        try {
            log.info("Test email request received");
            emailService.sendTestEmail();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to send test email: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/test-fraud-alert")
    public ResponseEntity<Map<String, String>> sendTestFraudAlert() {
        try {
            log.info("Test fraud alert request received");

            // Create sample fraud alerts for testing
            List<FraudAlert> testAlerts = List.of(
                    new FraudAlert(
                            "TEST-TXN-001",
                            "HIGH_RISK_COUNTRY_DETECTION",
                            "Transaction involves high-risk country",
                            "HIGH",
                            LocalDateTime.now(),
                            "TEST: Transaction from TEST-ACCOUNT-001 to TEST-ACCOUNT-002 involving high-risk country Afghanistan"
                    ),
                    new FraudAlert(
                            "TEST-TXN-002",
                            "THRESHOLD_AVOIDANCE_DETECTION",
                            "Transaction amount just below reporting threshold",
                            "MEDIUM",
                            LocalDateTime.now(),
                            "TEST: Transaction amount €9999.99 is suspiciously close to €10,000 reporting threshold"
                    )
            );

            notificationService.sendFraudAlerts(testAlerts);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test fraud alert sent successfully with " + testAlerts.size() + " alerts"
            ));
        } catch (Exception e) {
            log.error("Failed to send test fraud alert: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to send test fraud alert: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "fraud-alert-notification",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}