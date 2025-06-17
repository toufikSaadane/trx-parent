package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class HighAmountThresholdDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "HIGH_AMOUNT_THRESHOLD";
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("15000.00");

    @Override
    public FraudAlert detect(Transaction transaction) {
        BigDecimal threshold = configurationService.getHighAmountThreshold().orElse(DEFAULT_THRESHOLD);

        if (transaction.getAmount().compareTo(threshold) >= 0) {
            log.warn("High amount detected - Transaction: {}, Amount: {}, Threshold: {}",
                    transaction.getTransactionId(), transaction.getAmount(), threshold);

            return createAlert(
                    FraudAlert.FraudType.HIGH_AMOUNT_THRESHOLD,
                    String.format("Transaction amount %s exceeds threshold %s",
                            transaction.getAmount(), threshold),
                    7 // High severity
            );
        }

        return null;
    }

    @Override
    public String getDetectorName() {
        return DETECTOR_NAME;
    }
}