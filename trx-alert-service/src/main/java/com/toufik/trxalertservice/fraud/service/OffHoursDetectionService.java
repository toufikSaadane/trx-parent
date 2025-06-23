package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.FraudDetectionRule;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
public class OffHoursDetectionService implements FraudDetectionRule {

    // Off-hours defined as: 23:00-05:59 (11 PM to 6 AM)
    private static final LocalTime OFF_HOURS_START = LocalTime.of(23, 0);
    private static final LocalTime OFF_HOURS_END = LocalTime.of(6, 0);

    @Override
    public boolean isSuspicious(TransactionWithMT103Event event) {
        Transaction transaction = event.getTransaction();
        LocalDateTime timestamp = transaction.getTimestamp();
        LocalTime transactionTime = timestamp.toLocalTime();

        log.debug("Checking off-hours pattern for transaction {} - Time: {}",
                transaction.getTransactionId(), transactionTime);

        boolean isOffHours = isOffHoursTime(transactionTime);

        if (isOffHours) {
            log.warn("OFF-HOURS TRANSACTION DETECTED: Transaction {} occurred at {} - Outside normal business hours (06:00-23:00)",
                    transaction.getTransactionId(), timestamp);

            log.info("Off-Hours Transaction Details - Amount: {} {}, From: {} to {}, From Bank: {}, To Bank: {}",
                    transaction.getAmount(), transaction.getCurrency(),
                    transaction.getFromCountryCode(), transaction.getToCountryCode(),
                    transaction.getFromBankName(), transaction.getToBankName());
        }

        return isOffHours;
    }

    private boolean isOffHoursTime(LocalTime time) {
        return time.isAfter(OFF_HOURS_START.minusNanos(1)) || time.isBefore(OFF_HOURS_END);
    }

    @Override
    public String getRuleName() {
        return "OFF_HOURS_DETECTION";
    }

    @Override
    public String getDescription() {
        return "Detects transactions occurring during off-hours (23:00-05:59) - Unusual timing patterns";
    }
}