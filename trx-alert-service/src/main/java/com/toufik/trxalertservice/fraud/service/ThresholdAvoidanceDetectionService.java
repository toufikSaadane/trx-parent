package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.FraudDetectionRule;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class ThresholdAvoidanceDetectionService implements FraudDetectionRule {

    private static final BigDecimal REPORTING_THRESHOLD = new BigDecimal("10000.00");
    private static final BigDecimal SUSPICIOUS_EXACT_AMOUNT = new BigDecimal("9999.99");
    private static final BigDecimal THRESHOLD_RANGE_START = new BigDecimal("9900.00");
    private static final BigDecimal VERY_SUSPICIOUS_START = new BigDecimal("9990.00");

    @Override
    public boolean isSuspicious(TransactionWithMT103Event event) {
        Transaction transaction = event.getTransaction();
        BigDecimal amount = transaction.getAmount();

        log.debug("Checking threshold avoidance pattern for transaction {} with amount {}",
                transaction.getTransactionId(), amount);

        // Log the actual amount for debugging
        log.debug("Transaction amount details - Value: {}, Scale: {}, Precision: {}",
                amount, amount.scale(), amount.precision());

        // Check for exact suspicious amount (€9999.99)
        boolean isExactSuspiciousAmount = amount.compareTo(SUSPICIOUS_EXACT_AMOUNT) == 0;

        // Check for very suspicious range (€9990.00 - €9999.98)
        boolean isVerySuspicious = amount.compareTo(VERY_SUSPICIOUS_START) >= 0 &&
                amount.compareTo(SUSPICIOUS_EXACT_AMOUNT) < 0;

        // Check for suspicious range (€9900.00 - €9999.99)
        boolean isSuspiciousRange = amount.compareTo(THRESHOLD_RANGE_START) >= 0 &&
                amount.compareTo(REPORTING_THRESHOLD) < 0;

        if (isExactSuspiciousAmount) {
            log.warn("CRITICAL THRESHOLD AVOIDANCE: Transaction {} has highly suspicious amount {} (exactly €9999.99)",
                    transaction.getTransactionId(), amount);
        } else if (isVerySuspicious) {
            log.warn("HIGH THRESHOLD AVOIDANCE: Transaction {} amount {} is in very suspicious range (€9990-€9999.98)",
                    transaction.getTransactionId(), amount);
        } else if (isSuspiciousRange) {
            log.warn("POTENTIAL THRESHOLD AVOIDANCE: Transaction {} amount {} is just below €10,000 threshold",
                    transaction.getTransactionId(), amount);
        }

        boolean isSuspicious = isExactSuspiciousAmount || isVerySuspicious || isSuspiciousRange;

        if (isSuspicious) {
            log.info("Threshold Avoidance Details - Transaction: {}, Amount: {} {}, From: {} To: {}",
                    transaction.getTransactionId(), amount, transaction.getCurrency(),
                    transaction.getFromAccount(), transaction.getToAccount());

            // Additional context logging
            log.info("Account Details - From Bank: {}, To Bank: {}, Countries: {} -> {}",
                    transaction.getFromBankName(), transaction.getToBankName(),
                    transaction.getFromCountryCode(), transaction.getToCountryCode());
        } else {
            log.debug("Amount {} does not fall in suspicious threshold avoidance ranges", amount);
        }

        return isSuspicious;
    }

    @Override
    public String getRuleName() {
        return "THRESHOLD_AVOIDANCE_DETECTION";
    }

    @Override
    public String getDescription() {
        return "Detects transactions just below €10,000 reporting threshold (€9900-€9999.99)";
    }
}