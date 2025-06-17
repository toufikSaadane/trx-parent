package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.FraudScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FraudTransactionFactory extends BaseTransactionFactory implements TransactionFactory {

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

    // Store the selected fraud scenario and remittance text for MT103 formatting
    private ThreadLocal<FraudScenario> currentScenario = new ThreadLocal<>();
    private ThreadLocal<String> currentRemittanceText = new ThreadLocal<>();

    @Override
    public Transaction createTransaction() {
        return createBaseTransaction();
    }

    /**
     * Creates a transaction with applied fraud scenario
     */
    public Transaction createFraudTransaction(FraudScenario scenario, List<Transaction> recentTransactions) {
        log.debug("Creating fraud transaction for scenario: {}", scenario.getDescription());

        // Store scenario for later use in MT103 formatting
        currentScenario.set(scenario);
        currentRemittanceText.remove(); // Clear previous

        Transaction baseTransaction = createBaseTransaction();
        applyFraudScenario(baseTransaction, scenario, recentTransactions);

        log.info("Generated fraud transaction - Scenario: {}, Amount: {}, Timestamp: {}",
                scenario.getDescription(), baseTransaction.getAmount(), baseTransaction.getTimestamp());

        return baseTransaction;
    }

    @Override
    protected BigDecimal generateRandomAmount() {
        return AmountGenerationStrategy.SIMPLE_NORMAL.generateAmount(random);
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
        BigDecimal amount = HIGH_AMOUNT_THRESHOLD.add(
                BigDecimal.valueOf(random.nextDouble() * 10000)
        );
        transaction.setAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
        log.debug("Applied high amount: {}", transaction.getAmount());
    }

    private void applyOffHoursScenario(Transaction transaction) {
        LocalDateTime offHoursTime = LocalDateTime.now()
                .withHour(2 + random.nextInt(4))
                .withMinute(random.nextInt(60))
                .withSecond(random.nextInt(60));
        transaction.setTimestamp(offHoursTime);
        log.debug("Applied off-hours timestamp: {}", transaction.getTimestamp());
    }

    private void applySuspiciousRemittanceScenario(Transaction transaction) {
        String suspiciousText = SUSPICIOUS_REMITTANCE_KEYWORDS[random.nextInt(SUSPICIOUS_REMITTANCE_KEYWORDS.length)];
        currentRemittanceText.set(suspiciousText);
        log.debug("Applied suspicious remittance - Text: {}", suspiciousText);
    }

    private void applyRoundAmountScenario(Transaction transaction) {
        int[] roundAmounts = {1000, 2000, 5000, 10000, 20000, 50000};
        BigDecimal roundAmount = BigDecimal.valueOf(roundAmounts[random.nextInt(roundAmounts.length)]);
        transaction.setAmount(roundAmount);
        log.debug("Applied round amount: {}", transaction.getAmount());
    }

    private void applyFrequentSmallAmountsScenario(Transaction transaction) {
        BigDecimal smallAmount = BigDecimal.valueOf(100 + random.nextDouble() * 900);
        transaction.setAmount(smallAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
        log.debug("Applied frequent small amount: {}", transaction.getAmount());
    }

    private void applyCrossBorderHighRiskScenario(Transaction transaction) {
        BankInfo highRiskBank = findHighRiskBank();
        if (highRiskBank != null) {
            String toAccount = generateAccountNumber();
            String toIBAN = generateIBANForBank(highRiskBank, toAccount);

            transaction.setToAccount(toAccount);
            transaction.setToIBAN(toIBAN);
            transaction.setToBankSwift(highRiskBank.getSwiftCode());
            transaction.setToBankName(highRiskBank.getBankName());
            transaction.setToCountryCode(highRiskBank.getCountryCode());

            log.info("Applied high-risk destination - Country: {}, Bank: {}, SWIFT: {}",
                    highRiskBank.getCountryCode(),
                    highRiskBank.getBankName(),
                    highRiskBank.getSwiftCode());
        } else {
            log.warn("Failed to apply high-risk destination - no high-risk bank available");
        }
    }

    private void applyStructuringScenario(Transaction transaction) {
        transaction.setAmount(STRUCTURING_AMOUNT);
        log.debug("Applied structuring amount: {}", transaction.getAmount());
    }

    private void applyCryptocurrencyScenario(Transaction transaction) {
        String cryptoText = CRYPTOCURRENCY_KEYWORDS[random.nextInt(CRYPTOCURRENCY_KEYWORDS.length)];
        currentRemittanceText.set(cryptoText);
        log.debug("Applied cryptocurrency remittance - Text: {}", cryptoText);
    }

    /**
     * Gets the suspicious remittance text for MT103 formatting
     */
    public String getSuspiciousRemittanceText(String transactionId) {
        return currentRemittanceText.get();
    }

    /**
     * Finds a bank from high-risk countries - FIXED to actually use HIGH_RISK_COUNTRIES
     */
    private BankInfo findHighRiskBank() {
        List<String> highRiskCountries = Arrays.asList(HIGH_RISK_COUNTRIES);

        try {
            // Filter banks by HIGH_RISK_COUNTRIES - THIS WAS THE BUG
            List<BankInfo> highRiskBanks = bankDataService.getAllBanks().stream()
                    .filter(bank -> bank.getCountryCode() != null)
                    .filter(bank -> highRiskCountries.contains(bank.getCountryCode()))
                    .collect(Collectors.toList());

            if (!highRiskBanks.isEmpty()) {
                BankInfo selectedBank = highRiskBanks.get(random.nextInt(highRiskBanks.size()));
                log.debug("Selected high-risk bank from data: {} ({})",
                        selectedBank.getBankName(), selectedBank.getCountryCode());
                return selectedBank;
            }

            log.debug("No high-risk banks found in data, creating synthetic bank");
            return createSyntheticHighRiskBank();

        } catch (Exception e) {
            log.warn("Error finding high-risk bank from data: {}, creating synthetic bank", e.getMessage());
            return createSyntheticHighRiskBank();
        }
    }

    /**
     * Creates a synthetic bank from a high-risk country - FIXED to use HIGH_RISK_COUNTRIES
     */
    private BankInfo createSyntheticHighRiskBank() {
        String countryCode = HIGH_RISK_COUNTRIES[random.nextInt(HIGH_RISK_COUNTRIES.length)];

        BankInfo syntheticBank = new BankInfo();
        syntheticBank.setCountryCode(countryCode);
        syntheticBank.setSwiftCode(generateSyntheticSwiftCode(countryCode));
        syntheticBank.setBankName(generateSyntheticBankName(countryCode));

        log.info("Created synthetic high-risk bank - Country: {}, Name: {}, SWIFT: {}",
                countryCode, syntheticBank.getBankName(), syntheticBank.getSwiftCode());
        return syntheticBank;
    }

    /**
     * Generates a more realistic SWIFT code for synthetic high-risk banks
     */
    private String generateSyntheticSwiftCode(String countryCode) {
        String[] bankCodes = {"HRBN", "NATB", "INTL", "COMM", "FINC", "BNKH", "CRDT"};
        String bankCode = bankCodes[random.nextInt(bankCodes.length)];

        // Generate location code (usually XX for test/synthetic)
        String locationCode = "XX";

        // Some might have branch code
        String branchCode = random.nextBoolean() ? "XXX" : "";

        return bankCode + countryCode + locationCode + branchCode;
    }

    /**
     * Generates more realistic bank names for synthetic high-risk banks
     */
    private String generateSyntheticBankName(String countryCode) {
        String[] prefixes = {"National", "International", "Commercial", "First", "Central", "United", "Federal"};
        String[] suffixes = {"Bank", "Financial", "Trust", "Credit Union", "Banking Corp", "Finance Co", "Investment Bank"};

        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        String countryName = getCountryName(countryCode);

        return prefix + " " + countryName + " " + suffix;
    }

    /**
     * Gets country name from country code for synthetic bank creation
     */
    private String getCountryName(String countryCode) {
        return switch (countryCode) {
            case "AF" -> "Afghanistan";
            case "IR" -> "Iran";
            case "KP" -> "North Korea";
            case "MM" -> "Myanmar";
            case "SY" -> "Syria";
            case "YE" -> "Yemen";
            default -> "Unknown";
        };
    }
}