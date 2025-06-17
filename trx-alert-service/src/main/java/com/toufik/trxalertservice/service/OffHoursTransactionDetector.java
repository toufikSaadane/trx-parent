package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OffHoursTransactionDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "OFF_HOURS_TRANSACTION";

    @Override
    public FraudAlert detect(Transaction transaction) {
        int hour = transaction.getTimestamp().getHour();

        // Off-hours: 2 AM - 5 AM
        if (hour >= 2 && hour <= 5) {
            log.warn("Off-hours transaction detected - Transaction: {}, Time: {}",
                    transaction.getTransactionId(), transaction.getTimestamp());

            return createAlert(
                    FraudAlert.FraudType.OFF_HOURS_TRANSACTION,
                    String.format("Transaction occurred during off-hours at %s",
                            transaction.getTimestamp().toLocalTime()),
                    5 // Medium severity
            );
        }

        return null;
    }

    @Override
    public String getDetectorName() {
        return DETECTOR_NAME;
    }
}
