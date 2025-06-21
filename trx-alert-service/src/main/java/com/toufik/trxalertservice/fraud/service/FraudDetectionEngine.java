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

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEngine {

    private final HighRiskCountryDetectionService highRiskCountryDetectionService;
    private final ThresholdAvoidanceDetectionService thresholdAvoidanceDetectionService;

    public List<FraudAlert> detectFraud(TransactionWithMT103Event event) {
        String transactionId = event.getTransaction().getTransactionId();


        log.info("==================== FRAUD DETECTION STARTED ====================");
        log.info("Analyzing transaction: {}", transactionId);
        log.info("Transaction Amount: {} {}",
                event.getTransaction().getAmount(),
                event.getTransaction().getCurrency());
        log.info("From Country: {} | To Country: {}",
                event.getTransaction().getFromCountryCode(),
                event.getTransaction().getToCountryCode());

        List<FraudDetectionRule> rules = List.of(
                highRiskCountryDetectionService,
                thresholdAvoidanceDetectionService
        );

        List<FraudAlert> alerts = new ArrayList<>();
        for (FraudDetectionRule rule : rules) {
            log.debug("Executing fraud rule: {}", rule.getRuleName());

            if (rule.isSuspicious(event)) {
                FraudAlert alert = createFraudAlert(transactionId, rule);
                alerts.add(alert);

                log.warn("FRAUD RULE TRIGGERED: {} - {}", rule.getRuleName(), rule.getDescription());
            }
        }

        if (alerts.isEmpty()) {
            log.info("✅ No fraud patterns detected for transaction: {}", transactionId);
        } else {
            log.error("🚨 FRAUD DETECTED: {} suspicious patterns found for transaction: {}",
                    alerts.size(), transactionId);
        }

        log.info("==================== FRAUD DETECTION COMPLETED ==================");
        return alerts;
    }

    private FraudAlert createFraudAlert(String transactionId, FraudDetectionRule rule) {
        return new FraudAlert(
                transactionId,
                rule.getRuleName(),
                rule.getDescription(),
                determineSeverity(rule.getRuleName()),
                LocalDateTime.now(),
                String.format("Transaction %s triggered fraud rule: %s", transactionId, rule.getRuleName())
        );
    }

    private String determineSeverity(String ruleName) {
        return switch (ruleName) {
            case "HIGH_RISK_COUNTRY_DETECTION" -> "HIGH";
            case "THRESHOLD_AVOIDANCE_DETECTION" -> "HIGH";
            case "STRUCTURING_DETECTION" -> "MEDIUM";
            default -> "LOW";
        };
    }
}