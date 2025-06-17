package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class CryptocurrencyKeywordsDetector extends AbstractFraudDetector {

    private static final String DETECTOR_NAME = "CRYPTOCURRENCY_KEYWORDS";

    private static final List<String> CRYPTO_KEYWORDS = Arrays.asList(
            "bitcoin", "ethereum", "cryptocurrency", "crypto", "blockchain",
            "defi", "nft", "staking", "mining", "wallet", "exchange"
    );

    @Override
    public FraudAlert detect(Transaction transaction) {
        String textToAnalyze = extractTextToAnalyze(transaction);

        if (textToAnalyze != null && containsCryptoKeywords(textToAnalyze)) {
            log.warn("Cryptocurrency keywords detected - Transaction: {}", transaction.getTransactionId());

            return createAlert(
                    FraudAlert.FraudType.CRYPTOCURRENCY_KEYWORDS,
                    String.format("Transaction contains cryptocurrency-related keywords in bank names: %s",
                            textToAnalyze),
                    6 // Medium-High severity
            );
        }

        return null;
    }

    private String extractTextToAnalyze(Transaction transaction) {
        // Use bank names - simple and works with existing data
        String fromBank = transaction.getFromBankName();
        String toBank = transaction.getToBankName();

        if (fromBank != null && toBank != null) {
            return fromBank + " " + toBank;
        } else if (fromBank != null) {
            return fromBank;
        } else if (toBank != null) {
            return toBank;
        }

        return null;
    }

    private boolean containsCryptoKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return CRYPTO_KEYWORDS.stream()
                .anyMatch(lowerText::contains);
    }

    @Override
    public String getDetectorName() {
        return DETECTOR_NAME;
    }
}