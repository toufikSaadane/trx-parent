package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.FraudDetectionRule;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
@Slf4j
public class SuspiciousRemittanceDetectionService implements FraudDetectionRule {

    private static final Set<BigDecimal> SUSPICIOUS_PATTERN_AMOUNTS = Set.of(
            new BigDecimal("999"),
            new BigDecimal("9999"),
            new BigDecimal("99999"),
            new BigDecimal("999999")
    );

    @Override
    public boolean isSuspicious(TransactionWithMT103Event event) {
        Transaction transaction = event.getTransaction();
        BigDecimal amount = transaction.getAmount();

        log.debug("Checking suspicious remittance pattern for transaction {} - Amount: {}",
                transaction.getTransactionId(), amount);

        boolean isSuspiciousAmount = SUSPICIOUS_PATTERN_AMOUNTS.contains(amount);

        if (isSuspiciousAmount) {
            log.warn("SUSPICIOUS REMITTANCE PATTERN DETECTED: Transaction {} - Amount: {} matches pattern",
                    transaction.getTransactionId(), amount);

            log.info("Suspicious Remittance Details - From: {} to {}, From Bank: {}, To Bank: {}",
                    transaction.getFromCountryCode(), transaction.getToCountryCode(),
                    transaction.getFromBankName(), transaction.getToBankName());
        }

        return isSuspiciousAmount;
    }

    @Override
    public String getRuleName() {
        return "SUSPICIOUS_REMITTANCE_DETECTION";
    }

    @Override
    public String getDescription() {
        return "Detects transactions with suspicious pattern amounts (999, 9999, 99999, 999999)";
    }
}