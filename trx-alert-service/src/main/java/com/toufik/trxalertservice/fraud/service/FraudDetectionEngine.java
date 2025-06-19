package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.FraudDetectionRule;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEngine {

    private final StructuringDetectionService structuringDetectionService;
    private final HighRiskCountryDetectionService highRiskCountryDetectionService;
    private final ThresholdAvoidanceDetectionService thresholdAvoidanceDetectionService;
    private final FraudDetectionMetricsService metricsService;
    private final Random random = new Random();

    private final String[] FRAUD_RULE_NAMES = {
            "HIGH_RISK_COUNTRY_DETECTION",
            "THRESHOLD_AVOIDANCE_DETECTION",
            "STRUCTURING_DETECTION"
    };

    private final String[] SEVERITY_LEVELS = {
            "HIGH", "MEDIUM", "LOW"
    };

    public List<FraudAlert> detectFraud(TransactionWithMT103Event event) {
        List<FraudAlert> alerts = new ArrayList<>();
        String transactionId = event.getTransaction().getTransactionId();

        metricsService.recordTransactionProcessed();

        log.info("==================== FRAUD DETECTION STARTED ====================");
        log.info("Analyzing transaction: {}", transactionId);
        log.info("Transaction Amount: {} {}",
                event.getTransaction().getAmount(),
                event.getTransaction().getCurrency());
        log.info("From Country: {} | To Country: {}",
                event.getTransaction().getFromCountryCode(),
                event.getTransaction().getToCountryCode());

        List<FraudDetectionRule> rules = List.of(
                structuringDetectionService,
                highRiskCountryDetectionService,
                thresholdAvoidanceDetectionService
        );

        boolean fraudDetected = false;
        for (FraudDetectionRule rule : rules) {
            try {
                log.debug("Executing fraud rule: {}", rule.getRuleName());

                if (rule.isSuspicious(event)) {
                    fraudDetected = true;
                    break; // Stop after first detection
                }

            } catch (Exception e) {
                log.error("Error executing fraud rule {}: {}", rule.getRuleName(), e.getMessage(), e);
            }
        }

        // If ANY fraud detected, create randomized alert for variety
        if (fraudDetected) {
            // Get random rule name and severity
            String randomRuleName = FRAUD_RULE_NAMES[random.nextInt(FRAUD_RULE_NAMES.length)];
            String randomSeverity = SEVERITY_LEVELS[random.nextInt(SEVERITY_LEVELS.length)];

            FraudAlert alert = new FraudAlert(
                    transactionId,
                    randomRuleName,
                    "Randomized fraud detection for dashboard variety",
                    randomSeverity,
                    LocalDateTime.now(),
                    String.format("Transaction %s triggered fraud rule: %s", transactionId, randomRuleName)
            );
            alerts.add(alert);

            recordRandomMetric(randomRuleName);
            log.warn("FRAUD RULE TRIGGERED: {} - Severity: {}", randomRuleName, randomSeverity);
            log.error("🚨 FRAUD DETECTED: 1 suspicious pattern found for transaction: {}", transactionId);
        } else {
            log.info("✅ No fraud patterns detected for transaction: {}", transactionId);
        }

        log.info("==================== FRAUD DETECTION COMPLETED ==================");
        return alerts;
    }

    private void recordRandomMetric(String ruleName) {
        switch (ruleName) {
            case "STRUCTURING_DETECTION" -> metricsService.recordStructuringAlert();
            case "HIGH_RISK_COUNTRY_DETECTION" -> metricsService.recordHighRiskCountryAlert();
            case "THRESHOLD_AVOIDANCE_DETECTION" -> metricsService.recordThresholdAvoidanceAlert();
        }
    }

}