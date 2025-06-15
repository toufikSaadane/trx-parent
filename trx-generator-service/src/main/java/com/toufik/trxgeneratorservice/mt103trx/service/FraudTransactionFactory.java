package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.FraudScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@Slf4j
public class FraudTransactionFactory {

    @Autowired
    private BankDataService bankDataService;

    private final Random random = new Random();
    private final String[] transactionStatuses = {"PENDING", "COMPLETED", "PROCESSING"};

    // Fraud-specific constants
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("15000.00");
    private static final BigDecimal STRUCTURING_AMOUNT = new BigDecimal("9999.99");

    private static final String[] SUSPICIOUS_REMITTANCE_KEYWORDS = {
            "YieldFarm 500% guaranteed returns",
            "Crypto investment opportunity",
            "Urgent cash transfer required",
            "Invoice payment - confidential",
            "Investment return - mining pool",
            "Trading profit withdrawal",
            "Bonus payment - forex trading",
            "Commission payment - binary options"
    };

    private static final String[] CRYPTOCURRENCY_KEYWORDS = {
            "Bitcoin exchange transfer",
            "Ethereum wallet funding",
            "Cryptocurrency trading profit",
            "Digital asset conversion",
            "Blockchain investment return",
            "DeFi protocol payment",
            "NFT marketplace transaction",
            "Staking rewards withdrawal"
    };

    private static final String[] HIGH_RISK_COUNTRIES = {"AF", "IR", "KP", "MM", "SY", "YE"};

    /**
     * Creates a transaction with applied fraud scenario
     */
    public Transaction createFraudTransaction(FraudScenario scenario, List<Transaction> recentTransactions) {
        log.debug("Creating fraud transaction for scenario: {}", scenario.getDescription());

        Transaction baseTransaction = createBaseTransaction();
        applyFraudScenario(baseTransaction, scenario, recentTransactions);

        log.info("Generated fraud transaction - Scenario: {}, Amount: {}, Timestamp: {}",
                scenario.getDescription(), baseTransaction.getAmount(), baseTransaction.getTimestamp());

        return baseTransaction;
    }

    /**
     * Creates a base valid transaction
     */
    private Transaction createBaseTransaction() {
        BankInfo fromBank = bankDataService.getRandomBank();
        BankInfo toBank = getDistinctToBank(fromBank);

        String fromAccount = generateAccountNumber();
        String toAccount = generateAccountNumber();
        String fromIBAN = generateIBANForBank(fromBank, fromAccount);
        String toIBAN = generateIBANForBank(toBank, toAccount);

        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                fromAccount,
                toAccount,
                generateNormalAmount(),
                determineCurrency(fromBank, toBank),
                fromBank.getSwiftCode(),
                toBank.getSwiftCode(),
                fromBank.getBankName(),
                toBank.getBankName(),
                LocalDateTime.now(),
                transactionStatuses[random.nextInt(transactionStatuses.length)]
        );

        transaction.setFromIBAN(fromIBAN);
        transaction.setToIBAN(toIBAN);
        transaction.setFromCountryCode(fromBank.getCountryCode());
        transaction.setToCountryCode(toBank.getCountryCode());

        return transaction;
    }

    /**
     * Applies specific fraud scenario characteristics to the transaction
     */
    private void applyFraudScenario(Transaction transaction, FraudScenario scenario, List<Transaction> recentTransactions) {
        switch (scenario) {
            case HIGH_AMOUNT_THRESHOLD -> applyHighAmountScenario(transaction);
            case OFF_HOURS_TRANSACTION -> applyOffHoursScenario(transaction);
            case SUSPICIOUS_REMITTANCE -> applySuspiciousRemittanceScenario(transaction);
            case ROUND_AMOUNT_PATTERN -> applyRoundAmountScenario(transaction);
            case FREQUENT_SMALL_AMOUNTS -> applyFrequentSmallAmountsScenario(transaction);
            case CROSS_BORDER_HIGH_RISK -> applyCrossBorderHighRiskScenario(transaction);
            case STRUCTURING_PATTERN -> applyStructuringScenario(transaction);
            case CRYPTOCURRENCY_KEYWORDS -> applyCryptocurrencyScenario(transaction);
        }
    }

    private void applyHighAmountScenario(Transaction transaction) {
        // Set amount to exactly the high threshold or slightly above
        BigDecimal amount = HIGH_AMOUNT_THRESHOLD.add(
                BigDecimal.valueOf(random.nextDouble() * 10000)
        );
        transaction.setAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
        log.debug("Applied high amount: {}", transaction.getAmount());
    }

    private void applyOffHoursScenario(Transaction transaction) {
        // Set timestamp to suspicious hours (2 AM - 5 AM)
        LocalDateTime offHoursTime = LocalDateTime.now()
                .withHour(2 + random.nextInt(4))
                .withMinute(random.nextInt(60))
                .withSecond(random.nextInt(60));
        transaction.setTimestamp(offHoursTime);
        log.debug("Applied off-hours timestamp: {}", transaction.getTimestamp());
    }

    private void applySuspiciousRemittanceScenario(Transaction transaction) {
        // This will be handled by a custom MT103 formatter method
        // We'll add a flag to indicate suspicious remittance should be used
        transaction.setTransactionId("SUSP_" + transaction.getTransactionId());
        log.debug("Marked transaction for suspicious remittance");
    }

    private void applyRoundAmountScenario(Transaction transaction) {
        // Generate round amounts (multiples of 1000, 5000, etc.)
        int[] roundAmounts = {1000, 2000, 5000, 10000, 20000, 50000};
        BigDecimal roundAmount = BigDecimal.valueOf(roundAmounts[random.nextInt(roundAmounts.length)]);
        transaction.setAmount(roundAmount);
        log.debug("Applied round amount: {}", transaction.getAmount());
    }

    private void applyFrequentSmallAmountsScenario(Transaction transaction) {
        // Generate small amounts typically used in money laundering
        BigDecimal smallAmount = BigDecimal.valueOf(100 + random.nextDouble() * 900);
        transaction.setAmount(smallAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
        log.debug("Applied frequent small amount: {}", transaction.getAmount());
    }

    private void applyCrossBorderHighRiskScenario(Transaction transaction) {
        // Try to set destination to high-risk country
        BankInfo highRiskBank = findHighRiskBank();
        if (highRiskBank != null) {
            String toAccount = generateAccountNumber();
            String toIBAN = generateIBANForBank(highRiskBank, toAccount);

            transaction.setToAccount(toAccount);
            transaction.setToIBAN(toIBAN);
            transaction.setToBankSwift(highRiskBank.getSwiftCode());
            transaction.setToBankName(highRiskBank.getBankName());
            transaction.setToCountryCode(highRiskBank.getCountryCode());

            log.debug("Applied high-risk destination country: {}", highRiskBank.getCountryCode());
        }
    }

    private void applyStructuringScenario(Transaction transaction) {
        // Set amount just below reporting threshold
        transaction.setAmount(STRUCTURING_AMOUNT);
        log.debug("Applied structuring amount: {}", transaction.getAmount());
    }

    private void applyCryptocurrencyScenario(Transaction transaction) {
        // This will be handled by custom MT103 formatter
        // Mark transaction for cryptocurrency-related remittance
        transaction.setTransactionId("CRYPTO_" + transaction.getTransactionId());
        log.debug("Marked transaction for cryptocurrency remittance");
    }

    /**
     * Gets suspicious remittance text for marked transactions
     */
    public String getSuspiciousRemittanceText(String transactionId) {
        if (transactionId.startsWith("SUSP_")) {
            return SUSPICIOUS_REMITTANCE_KEYWORDS[random.nextInt(SUSPICIOUS_REMITTANCE_KEYWORDS.length)];
        } else if (transactionId.startsWith("CRYPTO_")) {
            return CRYPTOCURRENCY_KEYWORDS[random.nextInt(CRYPTOCURRENCY_KEYWORDS.length)];
        }
        return null;
    }

    // Helper methods (similar to TransactionGeneratorService)
    private BankInfo getDistinctToBank(BankInfo fromBank) {
        BankInfo toBank = bankDataService.getRandomBank();
        int attempts = 0;

        while (fromBank.getSwiftCode().equals(toBank.getSwiftCode()) && attempts < 10) {
            toBank = bankDataService.getRandomBank();
            attempts++;
        }

        return toBank;
    }

    private BankInfo findHighRiskBank() {
        return bankDataService.getRandomBank(); // Simplified - in real implementation, filter by country codes
    }

    private String generateIBANForBank(BankInfo bank, String accountNumber) {
        try {
            String iban = bankDataService.generateIBAN(bank, accountNumber);
            if (iban != null) {
                return iban;
            } else {
                log.warn("Could not generate IBAN for bank {}", bank.getSwiftCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error generating IBAN for bank {}: {}", bank.getSwiftCode(), e.getMessage());
            return null;
        }
    }

    private String determineCurrency(BankInfo fromBank, BankInfo toBank) {
        if (isEuropeanBank(fromBank) && isEuropeanBank(toBank)) {
            return "EUR";
        } else if (isUSBank(fromBank) || isUSBank(toBank)) {
            return "USD";
        }
        return fromBank.getCurrencyCode();
    }

    private boolean isEuropeanBank(BankInfo bank) {
        return bank.getCountryCode().matches("DE|FR|IT|ES|NL|BE|AT|PT|IE|FI|LU");
    }

    private boolean isUSBank(BankInfo bank) {
        return "US".equals(bank.getCountryCode());
    }

    private String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();
        int length = random.nextDouble() < 0.7 ? 12 : 10;

        for (int i = 0; i < length; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    private BigDecimal generateNormalAmount() {
        double amount = 100.0 + (random.nextDouble() * 5000.0);
        return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
    }
}