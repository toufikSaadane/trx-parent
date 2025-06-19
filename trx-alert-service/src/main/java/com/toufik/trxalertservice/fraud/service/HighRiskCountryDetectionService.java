package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.FraudDetectionRule;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.Map;

@Service
@Slf4j
public class HighRiskCountryDetectionService implements FraudDetectionRule {

    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "AF", // Afghanistan
            "IR", // Iran
            "KP", // North Korea
            "MM", // Myanmar
            "SY", // Syria
            "YE"  // Yemen
    );

    private static final Map<String, String> COUNTRY_NAMES = Map.of(
            "AF", "Afghanistan",
            "IR", "Iran",
            "KP", "North Korea",
            "MM", "Myanmar",
            "SY", "Syria",
            "YE", "Yemen"
    );

    @Override
    public boolean isSuspicious(TransactionWithMT103Event event) {
        Transaction transaction = event.getTransaction();
        String toCountryCode = transaction.getToCountryCode();
        String fromCountryCode = transaction.getFromCountryCode();

        log.debug("Checking high-risk country pattern for transaction {} - From: {}, To: {}",
                transaction.getTransactionId(), fromCountryCode, toCountryCode);

        boolean isHighRiskDestination = HIGH_RISK_COUNTRIES.contains(toCountryCode);
        boolean isHighRiskSource = HIGH_RISK_COUNTRIES.contains(fromCountryCode);

        if (isHighRiskDestination || isHighRiskSource) {
            String riskType = isHighRiskSource && isHighRiskDestination ? "SOURCE & DESTINATION" :
                    isHighRiskSource ? "SOURCE" : "DESTINATION";

            String sourceName = COUNTRY_NAMES.getOrDefault(fromCountryCode, fromCountryCode);
            String destName = COUNTRY_NAMES.getOrDefault(toCountryCode, toCountryCode);

            log.warn("HIGH-RISK COUNTRY DETECTED ({}): Transaction {} - From: {} ({}), To: {} ({})",
                    riskType, transaction.getTransactionId(),
                    fromCountryCode, sourceName, toCountryCode, destName);

            log.info("High-Risk Transaction Details - Amount: {} {}, From Bank: {}, To Bank: {}",
                    transaction.getAmount(), transaction.getCurrency(),
                    transaction.getFromBankName(), transaction.getToBankName());
        }

        return isHighRiskDestination || isHighRiskSource;
    }

    @Override
    public String getRuleName() {
        return "HIGH_RISK_COUNTRY_DETECTION";
    }

    @Override
    public String getDescription() {
        return "Detects transactions to/from high-risk countries (AF, IR, KP, MM, SY, YE)";
    }
}