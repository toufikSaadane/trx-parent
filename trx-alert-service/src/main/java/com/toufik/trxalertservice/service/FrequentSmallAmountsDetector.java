package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class FrequentSmallAmountsDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "FREQUENT_SMALL_AMOUNTS";
    private static final BigDecimal SMALL_AMOUNT_THRESHOLD = new BigDecimal("1000.00");

    @Override
    public FraudAlert detect(Transaction transaction) {
        // Check if transaction amount is small (potential structuring)
        if (transaction.getAmount().compareTo(SMALL_AMOUNT_THRESHOLD) < 0) {
            log.warn("Small amount detected for potential structuring - Transaction: {}, Amount: {}",
                    transaction.getTransactionId(), transaction.getAmount());

            return createAlert(
                    FraudAlert.FraudType.FREQUENT_SMALL_AMOUNTS,
                    String.format("Small transaction amount %s may indicate structuring behavior",
                            transaction.getAmount()),
                    4 // Medium severity
            );
        }

        return null;
    }

    @Override
    public String getDetectorName() {
        return DETECTOR_NAME;
    }
}
