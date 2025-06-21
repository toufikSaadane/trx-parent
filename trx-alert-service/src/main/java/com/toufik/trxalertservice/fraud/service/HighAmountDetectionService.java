package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.FraudDetectionRule;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class HighAmountDetectionService implements FraudDetectionRule {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("1000000");

    @Override
    public boolean isSuspicious(TransactionWithMT103Event event) {
        Transaction transaction = event.getTransaction();
        BigDecimal amount = transaction.getAmount();
        String currency = transaction.getCurrency();

        log.debug("Checking high amount pattern for transaction {} - Amount: {} {}",
                transaction.getTransactionId(), amount, currency);

        // Convert to EUR equivalent if needed (simplified - assumes EUR for now)
        BigDecimal amountInEur = convertToEur(amount, currency);

        boolean isHighAmount = amountInEur.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0;

        if (isHighAmount) {
            log.warn("HIGH AMOUNT DETECTED: Transaction {} - Amount: {} {} (EUR equivalent: {}) - Exceeds 1M threshold",
                    transaction.getTransactionId(), amount, currency, amountInEur);

            log.info("High Amount Transaction Details - From: {} to {}, From Bank: {}, To Bank: {}",
                    transaction.getFromCountryCode(), transaction.getToCountryCode(),
                    transaction.getFromBankName(), transaction.getToBankName());
        }

        return isHighAmount;
    }

    private BigDecimal convertToEur(BigDecimal amount, String currency) {
        // Simplified conversion - in production, you'd use a real exchange rate service
        return switch (currency.toUpperCase()) {
            case "EUR" -> amount;
            case "USD" -> amount.multiply(new BigDecimal("0.85")); // Approximate rate
            case "GBP" -> amount.multiply(new BigDecimal("1.15")); // Approximate rate
            case "CHF" -> amount.multiply(new BigDecimal("0.92")); // Approximate rate
            default -> amount; // Assume EUR if unknown currency
        };
    }

    @Override
    public String getRuleName() {
        return "HIGH_AMOUNT_DETECTION";
    }

    @Override
    public String getDescription() {
        return "Detects transactions with amounts ≥ 1,000,000 (or equivalent) - Large value transfers";
    }
}