package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class SuspiciousRemittanceDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "SUSPICIOUS_REMITTANCE";

    private static final List<String> SUSPICIOUS_KEYWORDS = Arrays.asList(
            "yieldfarm", "guaranteed returns", "crypto investment", "urgent cash",
            "confidential", "mining pool", "trading profit", "forex trading",
            "binary options", "investment opportunity"
    );

    @Override
    public FraudAlert detect(Transaction transaction) {
        String textToAnalyze = extractTextToAnalyze(transaction);

        if (textToAnalyze != null && containsSuspiciousKeywords(textToAnalyze)) {
            log.warn("Suspicious remittance detected - Transaction: {}", transaction.getTransactionId());

            return createAlert(
                    FraudAlert.FraudType.SUSPICIOUS_REMITTANCE,
                    String.format("Bank information contains suspicious keywords: %s",
                            textToAnalyze),
                    6 // Medium-High severity
            );
        }

        return null;
    }

    private String extractTextToAnalyze(Transaction transaction) {
        // Use bank names - simple and works with existing data
        String fromBank = transaction.getFromBankName();
        String toBank = transaction.getToBankName();

        if (fromBank != null && toBank != null) {
            return fromBank + " " + toBank;
        } else if (fromBank != null) {
            return fromBank;
        } else if (toBank != null) {
            return toBank;
        }

        return null;
    }

    private boolean containsSuspiciousKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return SUSPICIOUS_KEYWORDS.stream()
                .anyMatch(lowerText::contains);
    }

    @Override
    public String getDetectorName() {
        return DETECTOR_NAME;
    }
}