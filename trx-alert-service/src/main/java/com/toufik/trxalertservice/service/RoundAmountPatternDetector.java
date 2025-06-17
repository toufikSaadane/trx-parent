package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class RoundAmountPatternDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "ROUND_AMOUNT_PATTERN";

    @Override
    public FraudAlert detect(Transaction transaction) {
        if (isRoundAmount(transaction.getAmount())) {
            log.warn("Round amount pattern detected - Transaction: {}, Amount: {}",
                    transaction.getTransactionId(), transaction.getAmount());

            return createAlert(
                    FraudAlert.FraudType.ROUND_AMOUNT_PATTERN,
                    String.format("Transaction amount %s follows suspicious round pattern",
                            transaction.getAmount()),
                    4 // Medium severity
            );
        }

        return null;
    }

    private boolean isRoundAmount(BigDecimal amount) {
        double amountValue = amount.doubleValue();
        return amountValue % 1000 == 0 || amountValue % 5000 == 0 || amountValue % 2000 == 0;
    }

    @Override
    public String getDetectorName() {
        return DETECTOR_NAME;
    }
}
