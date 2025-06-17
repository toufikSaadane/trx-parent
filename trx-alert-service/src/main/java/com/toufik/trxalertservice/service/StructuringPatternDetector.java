package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class StructuringPatternDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "STRUCTURING_PATTERN";
    private static final BigDecimal STRUCTURING_THRESHOLD = new BigDecimal("10000.00");
    private static final BigDecimal STRUCTURING_MARGIN = new BigDecimal("100.00");

    @Override
    public FraudAlert detect(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        BigDecimal lowerBound = STRUCTURING_THRESHOLD.subtract(STRUCTURING_MARGIN);

        // Check if amount is just under the reporting threshold
        if (amount.compareTo(lowerBound) >= 0 && amount.compareTo(STRUCTURING_THRESHOLD) < 0) {
            log.warn("Structuring pattern detected - Transaction: {}, Amount: {}",
                    transaction.getTransactionId(), amount);

            return createAlert(
                    FraudAlert.FraudType.STRUCTURING_PATTERN,
                    String.format("Transaction amount %s appears to be structured to avoid reporting threshold",
                            amount),
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
