package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.config.FraudConfigurationService;
import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.FraudDetectionResult;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FraudDetectionService {

    @Autowired
    private List<FraudDetector> fraudDetectors;

    @Autowired
    private FraudConfigurationService configurationService;

    public FraudDetectionResult detectFraud(Transaction transaction) {
        log.info("Starting fraud detection for transaction: {}", transaction.getTransactionId());

        List<FraudAlert> alerts = fraudDetectors.stream()
                .filter(detector -> detector.isEnabled())
                .map(detector -> detector.detect(transaction))
                .filter(alert -> alert != null)
                .collect(Collectors.toList());

        int totalRiskScore = calculateRiskScore(alerts);
        FraudDetectionResult.RiskLevel riskLevel = FraudDetectionResult.RiskLevel.fromScore(totalRiskScore);

        boolean isFraudulent = totalRiskScore >= configurationService.getFraudThreshold();

        FraudDetectionResult result = FraudDetectionResult.builder()
                .transactionId(transaction.getTransactionId())
                .isFraudulent(isFraudulent)
                .riskScore(totalRiskScore)
                .riskLevel(riskLevel)
                .alerts(alerts)
                .detectionTimestamp(LocalDateTime.now())
                .build();

        log.info("Fraud detection completed - Transaction: {}, Risk Score: {}, Fraudulent: {}",
                transaction.getTransactionId(), totalRiskScore, isFraudulent);

        return result;
    }

    private int calculateRiskScore(List<FraudAlert> alerts) {
        return alerts.stream()
                .mapToInt(FraudAlert::getSeverity)
                .sum() * 10; // Scale severity to 0-100 range
    }
}