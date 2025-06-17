package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class CrossBorderHighRiskDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "CROSS_BORDER_HIGH_RISK";

    private static final List<String> HIGH_RISK_COUNTRIES = Arrays.asList(
            "AF", "IR", "KP", "MM", "SY", "YE"
    );

    @Override
    public FraudAlert detect(Transaction transaction) {
        String toCountry = transaction.getToCountryCode();
        String fromCountry = transaction.getFromCountryCode();

        if (isHighRiskCountry(toCountry) || isHighRiskCountry(fromCountry)) {
            log.warn("High-risk country detected - Transaction: {}, From: {}, To: {}",
                    transaction.getTransactionId(), fromCountry, toCountry);

            return createAlert(
                    FraudAlert.FraudType.CROSS_BORDER_HIGH_RISK,
                    String.format("Transaction involves high-risk country. From: %s, To: %s",
                            fromCountry, toCountry),
                    8 // High severity
            );
        }

        return null;
    }

    private boolean isHighRiskCountry(String countryCode) {
        return countryCode != null && HIGH_RISK_COUNTRIES.contains(countryCode);
    }

    @Override
    public String getDetectorName() {
        return DETECTOR_NAME;
    }
}
