package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransactionGeneratorService extends BaseTransactionFactory implements TransactionFactory {

    private final TransactionProducer transactionProducer;
    private final MT103MessageFormatter mt103MessageFormatter;

    public TransactionGeneratorService(TransactionProducer transactionProducer,
                                       @Qualifier("MT103MessageFormatter") MT103MessageFormatter mt103MessageFormatter) {
        this.transactionProducer = transactionProducer;
        this.mt103MessageFormatter = mt103MessageFormatter;
    }

    @Scheduled(fixedRate = 10000)
    public void generateAndSendTransaction() {
        try {
            TransactionWithMT103Event transactionWithMT103Event = generateRandomTransactionWithMT103();
            log.info("Generated transaction: {}", transactionWithMT103Event.getTransaction().getTransactionId());
            log.info("======================= TRANSACTION =============================");
            log.info("Transaction Details: {}", transactionWithMT103Event.getTransaction());
            log.info("From IBAN: {}", transactionWithMT103Event.getTransaction().getFromIBAN());
            log.info("To IBAN: {}", transactionWithMT103Event.getTransaction().getToIBAN());
            transactionProducer.sendTransaction(transactionWithMT103Event);
        } catch (Exception e) {
            log.error("Error generating transaction: {}", e.getMessage(), e);
        }
    }

    @Override
    public Transaction createTransaction() {
        return createBaseTransaction();
    }

    public Transaction generateRandomTransaction() {
        return createTransaction();
    }

    private TransactionWithMT103Event generateRandomTransactionWithMT103() {
        Transaction transaction = generateRandomTransaction();
        String mt103Content = mt103MessageFormatter.formatToMT103(transaction);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(transaction);
        result.setMt103Content(mt103Content);

        return result;
    }

    @Override
    protected BigDecimal generateRandomAmount() {
        return AmountGenerationStrategy.NORMAL.generateAmount(random);
    }
}
===
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.FraudScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
public class FraudTransactionGeneratorService {

    @Autowired
    private FraudTransactionFactory fraudTransactionFactory;

    @Autowired
    private TransactionProducer transactionProducer;

    @Autowired
    private FraudMT103MessageFormatter fraudMT103MessageFormatter;

    private final Random random = new Random();
    private final ConcurrentLinkedQueue<Transaction> transactionHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_HISTORY_SIZE = 1000;

    // Weighted fraud scenario selection
    private static final FraudScenario[] FRAUD_SCENARIOS = FraudScenario.values();
    private static final int[] SCENARIO_WEIGHTS = {
            15, // HIGH_AMOUNT_THRESHOLD - 15%
            10, // OFF_HOURS_TRANSACTION - 10%
            20, // SUSPICIOUS_REMITTANCE - 20%
            15, // ROUND_AMOUNT_PATTERN - 15%
            10, // FREQUENT_SMALL_AMOUNTS - 10%
            15, // CROSS_BORDER_HIGH_RISK - 15%
            10, // STRUCTURING_PATTERN - 10%
            5   // CRYPTOCURRENCY_KEYWORDS - 5%
    };

    /**
     * Scheduled method to generate fraud transactions every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void generateAndSendFraudTransaction() {
        try {
            FraudScenario selectedScenario = selectWeightedFraudScenario();
            log.info("Generating fraud transaction with scenario: {}", selectedScenario.getDescription());

            TransactionWithMT103Event fraudTransactionEvent = generateFraudTransactionWithMT103(selectedScenario);

            // Add to history for pattern-based analysis
            addToHistory(fraudTransactionEvent.getTransaction());

            log.info("Generated fraud transaction: {} - Scenario: {}",
                    fraudTransactionEvent.getTransaction().getTransactionId(),
                    selectedScenario.getDescription());

            // Enhanced logging for fraud transaction details
            logFraudTransactionDetails(fraudTransactionEvent.getTransaction(), selectedScenario);

            transactionProducer.sendTransaction(fraudTransactionEvent);

        } catch (Exception e) {
            log.error("Error generating fraud transaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Selects a fraud scenario based on weighted probabilities
     */
    private FraudScenario selectWeightedFraudScenario() {
        int totalWeight = Arrays.stream(SCENARIO_WEIGHTS).sum();
        int randomValue = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (int i = 0; i < FRAUD_SCENARIOS.length; i++) {
            currentWeight += SCENARIO_WEIGHTS[i];
            if (randomValue < currentWeight) {
                return FRAUD_SCENARIOS[i];
            }
        }

        // Fallback to first scenario
        return FRAUD_SCENARIOS[0];
    }

    /**
     * Generates a fraud transaction with MT103 message
     */
    private TransactionWithMT103Event generateFraudTransactionWithMT103(FraudScenario scenario) {
        // Get recent transactions for pattern-based scenarios
        List<Transaction> recentTransactions = getRecentTransactions();

        // Generate the fraud transaction
        Transaction fraudTransaction = fraudTransactionFactory.createFraudTransaction(scenario, recentTransactions);

        // Create the event
        TransactionWithMT103Event event = new TransactionWithMT103Event();
        event.setTransaction(fraudTransaction);

        return event;
    }

    /**
     * Enhanced logging for fraud transaction details
     */
    private void logFraudTransactionDetails(Transaction transaction, FraudScenario scenario) {
        log.info("======================= FRAUD TRANSACTION =============================");
        log.info("Fraud Scenario: {} - {}", scenario.name(), scenario.getDescription());
        log.info("Transaction ID: {}", transaction.getTransactionId());
        log.info("Amount: {} {}", transaction.getAmount(), transaction.getCurrency());
        log.info("Timestamp: {}", transaction.getTimestamp());

        // Log party information
        log.info("From Bank: {} ({})", transaction.getFromBankName(), transaction.getFromBankSwift());
        log.info("From IBAN: {}", transaction.getFromIBAN());
        log.info("To Bank: {} ({})", transaction.getToBankName(), transaction.getToBankSwift());
        log.info("To IBAN: {}", transaction.getToIBAN());


        // Log scenario-specific details
        logScenarioSpecificDetails(transaction, scenario);

        log.info("===================================================================");
    }

    /**
     * Logs scenario-specific details
     */
    private void logScenarioSpecificDetails(Transaction transaction, FraudScenario scenario) {
        switch (scenario) {
            case HIGH_AMOUNT_THRESHOLD ->
                    log.info("High Amount Alert: {} exceeds threshold", transaction.getAmount());

            case OFF_HOURS_TRANSACTION ->
                    log.info("Off-Hours Alert: Transaction at {}", transaction.getTimestamp().toLocalTime());

            case SUSPICIOUS_REMITTANCE ->
                    log.info("Suspicious Remittance Alert: Contains suspicious keywords");

            case ROUND_AMOUNT_PATTERN ->
                    log.info("Round Amount Alert: Exact round amount {}", transaction.getAmount());

            case FREQUENT_SMALL_AMOUNTS ->
                    log.info("Small Amount Alert: Potential structuring behavior");

            case CROSS_BORDER_HIGH_RISK ->
                    log.info("High-Risk Country Alert: Destination country {}", transaction.getToCountryCode());

            case STRUCTURING_PATTERN ->
                    log.info("Structuring Alert: Amount just under reporting threshold");

            case CRYPTOCURRENCY_KEYWORDS ->
                    log.info("Cryptocurrency Alert: Contains crypto-related keywords");
        }
    }

    /**
     * Adds transaction to history and manages size
     */
    private void addToHistory(Transaction transaction) {
        transactionHistory.offer(transaction);

        // Remove oldest entries if history exceeds max size
        while (transactionHistory.size() > MAX_HISTORY_SIZE) {
            transactionHistory.poll();
        }

        log.debug("Transaction history size: {}", transactionHistory.size());
    }

    /**
     * Gets recent transactions for pattern analysis
     */
    private List<Transaction> getRecentTransactions() {
        return new ArrayList<>(transactionHistory);
    }

    /**
     * Manual trigger for generating specific fraud scenario (for testing)
     */
    public TransactionWithMT103Event generateSpecificFraudTransaction(FraudScenario scenario) {
        log.info("Manually generating fraud transaction for scenario: {}", scenario.getDescription());

        TransactionWithMT103Event event = generateFraudTransactionWithMT103(scenario);
        addToHistory(event.getTransaction());

        return event;
    }

    /**
     * Gets current transaction history size
     */
    public int getHistorySize() {
        return transactionHistory.size();
    }

    /**
     * Clears transaction history (for testing/maintenance)
     */
    public void clearHistory() {
        transactionHistory.clear();
        log.info("Transaction history cleared");
    }
}
=====
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import static com.toufik.trxgeneratorservice.mt103trx.util.MT103Constants.*;

@Service("MT103MessageFormatter")
public class MT103MessageFormatter {

    /**
     * Formats a transaction into MT103 SWIFT message format
     */
    public String formatToMT103(Transaction transaction) {
        var mt103 = new StringBuilder();
        var transactionRef = truncateToLength(transaction.getTransactionId(), 16);

        // Build MT103 structure
        appendHeader(mt103, transaction);
        appendMessageText(mt103, transaction, transactionRef);
        appendTrailer(mt103);

        return mt103.toString();
    }

    /**
     * Appends the complete header blocks (1, 2, 3)
     */
    private void appendHeader(StringBuilder mt103, Transaction transaction) {
        var senderLTAddress = formatLTAddress(transaction.getFromBankSwift());
        var receiverLTAddress = formatLTAddress(transaction.getToBankSwift());
        var transactionRef = truncateToLength(transaction.getTransactionId(), 16);

        // Header Block 1: Basic Header
        mt103.append("{1:F01").append(senderLTAddress).append("}");

        // Header Block 2: Application Header (Input)
        mt103.append("{2:I103").append(receiverLTAddress).append("N}");

        // Header Block 3: User Header
        mt103.append("{3:{108:").append(transactionRef).append("}}");
    }

    /**
     * Appends the complete message text block (Block 4)
     */
    private void appendMessageText(StringBuilder mt103, Transaction transaction, String transactionRef) {
        mt103.append("\n{4:\n");

        appendMandatoryFields(mt103, transaction, transactionRef);
        appendAccountingFields(mt103, transaction);
        appendPartyFields(mt103, transaction);
        appendOptionalFields(mt103, transaction);

        mt103.append("}");
    }

    /**
     * Appends mandatory MT103 fields (20, 23B, 32A)
     */
    private void appendMandatoryFields(StringBuilder mt103, Transaction transaction, String transactionRef) {
        var valueDate = transaction.getTimestamp().format(DATE_FORMATTER);
        var formattedAmount = formatAmount(transaction.getAmount().toString());

        mt103.append(":20:").append(transactionRef).append("\n")
                .append(":23B:CRED\n")
                .append(":32A:").append(valueDate)
                .append(transaction.getCurrency())
                .append(formattedAmount).append("\n");
    }

    /**
     * Appends accounting-related fields (33B, 71A)
     */
    private void appendAccountingFields(StringBuilder mt103, Transaction transaction) {
        var formattedAmount = formatAmount(transaction.getAmount().toString());
        var chargeBearer = transaction.isCrossBorder() ? "SHA" : "OUR";

        mt103.append(":33B:").append(transaction.getCurrency())
                .append(formattedAmount).append("\n")
                .append(":71A:").append(chargeBearer).append("\n");
    }

    /**
     * Appends all party-related fields (50K, 52A, 53B, 56A, 57A, 59)
     */
    private void appendPartyFields(StringBuilder mt103, Transaction transaction) {
        appendOrderingCustomer(mt103, transaction);
        appendOrderingInstitution(mt103, transaction);
        appendSenderCorrespondent(mt103, transaction);
        appendIntermediaryBank(mt103, transaction);
        appendAccountWithInstitution(mt103, transaction);
        appendBeneficiaryCustomer(mt103, transaction);
    }

    private void appendOrderingCustomer(StringBuilder mt103, Transaction transaction) {
        mt103.append(":50K:");

        if (hasValidIBAN(transaction.getFromIBAN())) {
            mt103.append("/").append(transaction.getFromIBAN()).append("\n");
        } else {
            mt103.append("/").append(transaction.getFromAccount()).append("\n");
        }

        mt103.append(transaction.getFromBankName()).append("\n")
                .append(generateAddressLine()).append("\n")
                .append(generateCityCountry(transaction.getFromCountryCode())).append("\n");
    }

    private void appendOrderingInstitution(StringBuilder mt103, Transaction transaction) {
        mt103.append(":52A:").append(transaction.getFromBankSwift()).append("\n");
    }

    private void appendSenderCorrespondent(StringBuilder mt103, Transaction transaction) {
        mt103.append(":53B:/").append(transaction.getFromBankSwift()).append("\n");
    }

    private void appendIntermediaryBank(StringBuilder mt103, Transaction transaction) {
        if (needsIntermediaryBank(transaction)) {
            var intermediaryBIC = getIntermediaryBankSwift(transaction);
            mt103.append(":56A:").append(intermediaryBIC).append("\n");
        }
    }

    private void appendAccountWithInstitution(StringBuilder mt103, Transaction transaction) {
        mt103.append(":57A:").append(transaction.getToBankSwift()).append("\n");
    }

    private void appendBeneficiaryCustomer(StringBuilder mt103, Transaction transaction) {
        mt103.append(":59:");

        if (hasValidIBAN(transaction.getToIBAN())) {
            mt103.append("/").append(transaction.getToIBAN()).append("\n");
        } else {
            mt103.append("/").append(transaction.getToAccount()).append("\n");
        }

        mt103.append(transaction.getToBankName()).append("\n")
                .append(generateAddressLine()).append("\n")
                .append(generateCityCountry(transaction.getToCountryCode())).append("\n");
    }

    /**
     * Appends optional fields (70, 72)
     */
    private void appendOptionalFields(StringBuilder mt103, Transaction transaction) {
        var remittanceInfo = buildRemittanceInfo(transaction);

        mt103.append(":70:").append(remittanceInfo).append("\n")
                .append(":72:/INS/").append(transaction.getFromBankSwift()).append("\n");
    }

    /**
     * Appends trailer block (Block 5)
     */
    private void appendTrailer(StringBuilder mt103) {
        mt103.append("\n{5:{MAC:").append(generateMAC())
                .append("}{CHK:").append(generateChecksum()).append("}}");
    }

    /**
     * Formats BIC to LT Address format
     */
    private String formatLTAddress(String bic) {
        var normalizedBIC = bic.trim().toUpperCase();

        return switch (normalizedBIC.length()) {
            case 8 -> normalizedBIC + "XXX0";
            case 11 -> normalizedBIC + "0";
            case 12 -> normalizedBIC;
            default -> normalizedBIC + "0";
        };
    }

    /**
     * Formats amount by replacing decimal point with comma
     */
    private String formatAmount(String amount) {
        return amount.replace(".", ",");
    }

    /**
     * Truncates string to specified length
     */
    private String truncateToLength(String input, int maxLength) {
        if (input == null) return "";
        return input.length() > maxLength ? input.substring(0, maxLength) : input;
    }

    /**
     * Determines if an intermediary bank is needed for cross-border transactions
     */
    private boolean needsIntermediaryBank(Transaction transaction) {
        try {
            var fromCountry = extractCountryCode(transaction.getFromBankSwift());
            var toCountry = extractCountryCode(transaction.getToBankSwift());
            return !fromCountry.equals(toCountry);
        } catch (StringIndexOutOfBoundsException e) {
            return true;
        }
    }

    /**
     * Extracts country code from SWIFT BIC
     */
    private String extractCountryCode(String swiftCode) {
        return swiftCode.substring(4, 6);
    }

    /**
     * Gets appropriate intermediary bank SWIFT code
     */
    private String getIntermediaryBankSwift(Transaction transaction) {
        try {
            var toCountry = extractCountryCode(transaction.getToBankSwift());
            return INTERMEDIARY_BANKS.getOrDefault(toCountry, "DEUTDEFFXXX");
        } catch (StringIndexOutOfBoundsException e) {
            return "DEUTDEFFXXX";
        }
    }

    /**
     * Builds remittance information string
     */
    private String buildRemittanceInfo(Transaction transaction) {
        var shortTransactionId = truncateToLength(transaction.getTransactionId(), 8);
        var remittance = new StringBuilder()
                .append("Payment for services - TXN ID: ").append(shortTransactionId);

        if (transaction.isCrossBorder()) {
            remittance.append(" - Cross-border transfer");
        }

        return remittance.toString();
    }

    /**
     * Checks if IBAN is valid (not null or empty)
     */
    private boolean hasValidIBAN(String iban) {
        return iban != null && !iban.isEmpty() && !iban.equals("This country does not use IBAN");
    }

    /**
     * Generates a random address line
     */
    private String generateAddressLine() {
        return ADDRESS_TEMPLATES[ThreadLocalRandom.current().nextInt(ADDRESS_TEMPLATES.length)];
    }

    /**
     * Generates city and country string based on country code
     */
    private String generateCityCountry(String countryCode) {
        if (countryCode == null) {
            return "Unknown City, Unknown Country";
        }

        return COUNTRY_CITIES.getOrDefault(
                countryCode,
                "Financial District, " + getCountryName(countryCode)
        );
    }

    /**
     * Gets full country name from country code
     */
    private String getCountryName(String countryCode) {
        return ADDITIONAL_COUNTRIES.getOrDefault(countryCode, "Unknown Country");
    }

    /**
     * Generates realistic MAC (Message Authentication Code)
     */
    private String generateMAC() {
        return generateHexString(8);
    }

    /**
     * Generates realistic checksum
     */
    private String generateChecksum() {
        return generateHexString(12);
    }

    /**
     * Generates random hexadecimal string of specified length
     */
    private String generateHexString(int length) {
        var result = new StringBuilder();
        var random = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            result.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }

        return result.toString();
    }
}
====
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import static com.toufik.trxgeneratorservice.mt103trx.util.MT103Constants.*;

@Service
public class FraudMT103MessageFormatter extends MT103MessageFormatter {

    private final FraudTransactionFactory fraudTransactionFactory;

    public FraudMT103MessageFormatter(FraudTransactionFactory fraudTransactionFactory) {
        this.fraudTransactionFactory = fraudTransactionFactory;
    }

    /**
     * Formats a fraud transaction into MT103 SWIFT message format
     * Extends the base formatter with fraud-specific remittance information
     */
    @Override
    public String formatToMT103(Transaction transaction) {
        var mt103 = new StringBuilder();
        var transactionRef = truncateToLength(transaction.getTransactionId(), 16);

        // Build MT103 structure using base class methods
        appendHeader(mt103, transaction);
        appendMessageTextWithFraudInfo(mt103, transaction, transactionRef);
        appendTrailer(mt103);

        return mt103.toString();
    }

    /**
     * Appends the complete message text block with fraud-specific information
     */
    private void appendMessageTextWithFraudInfo(StringBuilder mt103, Transaction transaction, String transactionRef) {
        mt103.append("\n{4:\n");

        appendMandatoryFields(mt103, transaction, transactionRef);
        appendAccountingFields(mt103, transaction);
        appendPartyFields(mt103, transaction);
        appendFraudSpecificOptionalFields(mt103, transaction);

        mt103.append("}");
    }

    /**
     * Appends optional fields with fraud-specific remittance information
     */
    private void appendFraudSpecificOptionalFields(StringBuilder mt103, Transaction transaction) {
        var remittanceInfo = buildFraudRemittanceInfo(transaction);

        mt103.append(":70:").append(remittanceInfo).append("\n")
                .append(":72:/INS/").append(transaction.getFromBankSwift()).append("\n");
    }

    /**
     * Builds fraud-specific remittance information
     */
    private String buildFraudRemittanceInfo(Transaction transaction) {
        String suspiciousText = fraudTransactionFactory.getSuspiciousRemittanceText(transaction.getTransactionId());

        if (suspiciousText != null) {
            return suspiciousText;
        }

        return buildCharacteristicBasedRemittance(transaction);
    }

    /**
     * Builds remittance info based on transaction characteristics for fraud scenarios
     */
    private String buildCharacteristicBasedRemittance(Transaction transaction) {
        var shortTransactionId = truncateToLength(transaction.getTransactionId(), 8);
        var remittance = new StringBuilder();

        // High amount transactions
        if (transaction.getAmount().compareTo(java.math.BigDecimal.valueOf(15000)) >= 0) {
            remittance.append("Large value transfer - Ref: ").append(shortTransactionId);
        }
        // Round amount patterns
        else if (isRoundAmount(transaction.getAmount())) {
            remittance.append("Business payment - Invoice: ").append(shortTransactionId);
        }
        // Off-hours transactions (detected by time)
        else if (isOffHours(transaction.getTimestamp())) {
            remittance.append("Urgent payment required - Ref: ").append(shortTransactionId);
        }
        // Structuring amounts
        else if (transaction.getAmount().compareTo(java.math.BigDecimal.valueOf(9999)) >= 0 &&
                transaction.getAmount().compareTo(java.math.BigDecimal.valueOf(10000)) < 0) {
            remittance.append("Trade settlement - Contract: ").append(shortTransactionId);
        }
        // Small frequent amounts
        else if (transaction.getAmount().compareTo(java.math.BigDecimal.valueOf(1000)) < 0) {
            remittance.append("Service payment - Multiple invoices");
        }
        // Cross-border high-risk
        else if (transaction.isCrossBorder()) {
            remittance.append("International trade payment - Ref: ").append(shortTransactionId);
        }
        // Default fraud-like pattern
        else {
            remittance.append("Commercial payment - TXN: ").append(shortTransactionId);
        }

        return remittance.toString();
    }

    /**
     * Checks if amount follows a round pattern (multiples of 1000, 5000, etc.)
     */
    private boolean isRoundAmount(java.math.BigDecimal amount) {
        double amountValue = amount.doubleValue();
        return amountValue % 1000 == 0 || amountValue % 5000 == 0 || amountValue % 2000 == 0;
    }

    /**
     * Checks if transaction timestamp is during off-hours (2 AM - 5 AM)
     */
    private boolean isOffHours(java.time.LocalDateTime timestamp) {
        int hour = timestamp.getHour();
        return hour >= 2 && hour <= 5;
    }

    /**
     * Utility method to truncate strings (inherited from parent, redeclared for access)
     */
    private String truncateToLength(String input, int maxLength) {
        if (input == null) return "";
        return input.length() > maxLength ? input.substring(0, maxLength) : input;
    }

    /**
     * Appends mandatory MT103 fields - delegates to parent class logic
     */
    private void appendMandatoryFields(StringBuilder mt103, Transaction transaction, String transactionRef) {
        var valueDate = transaction.getTimestamp().format(DATE_FORMATTER);
        var formattedAmount = formatAmount(transaction.getAmount().toString());

        mt103.append(":20:").append(transactionRef).append("\n")
                .append(":23B:CRED\n")
                .append(":32A:").append(valueDate)
                .append(transaction.getCurrency())
                .append(formattedAmount).append("\n");
    }

    /**
     * Appends accounting-related fields - delegates to parent class logic
     */
    private void appendAccountingFields(StringBuilder mt103, Transaction transaction) {
        var formattedAmount = formatAmount(transaction.getAmount().toString());
        var chargeBearer = transaction.isCrossBorder() ? "SHA" : "OUR";

        mt103.append(":33B:").append(transaction.getCurrency())
                .append(formattedAmount).append("\n")
                .append(":71A:").append(chargeBearer).append("\n");
    }

    /**
     * Appends all party-related fields - delegates to parent class logic
     */
    private void appendPartyFields(StringBuilder mt103, Transaction transaction) {
        appendOrderingCustomer(mt103, transaction);
        appendOrderingInstitution(mt103, transaction);
        appendSenderCorrespondent(mt103, transaction);
        appendIntermediaryBank(mt103, transaction);
        appendAccountWithInstitution(mt103, transaction);
        appendBeneficiaryCustomer(mt103, transaction);
    }

    // Parent class method implementations (copied for access)
    private void appendOrderingCustomer(StringBuilder mt103, Transaction transaction) {
        mt103.append(":50K:");

        if (hasValidIBAN(transaction.getFromIBAN())) {
            mt103.append("/").append(transaction.getFromIBAN()).append("\n");
        } else {
            mt103.append("/").append(transaction.getFromAccount()).append("\n");
        }

        mt103.append(transaction.getFromBankName()).append("\n")
                .append(generateAddressLine()).append("\n")
                .append(generateCityCountry(transaction.getFromCountryCode())).append("\n");
    }

    private void appendOrderingInstitution(StringBuilder mt103, Transaction transaction) {
        mt103.append(":52A:").append(transaction.getFromBankSwift()).append("\n");
    }

    private void appendSenderCorrespondent(StringBuilder mt103, Transaction transaction) {
        mt103.append(":53B:/").append(transaction.getFromBankSwift()).append("\n");
    }

    private void appendIntermediaryBank(StringBuilder mt103, Transaction transaction) {
        if (needsIntermediaryBank(transaction)) {
            var intermediaryBIC = getIntermediaryBankSwift(transaction);
            mt103.append(":56A:").append(intermediaryBIC).append("\n");
        }
    }

    private void appendAccountWithInstitution(StringBuilder mt103, Transaction transaction) {
        mt103.append(":57A:").append(transaction.getToBankSwift()).append("\n");
    }

    private void appendBeneficiaryCustomer(StringBuilder mt103, Transaction transaction) {
        mt103.append(":59:");

        if (hasValidIBAN(transaction.getToIBAN())) {
            mt103.append("/").append(transaction.getToIBAN()).append("\n");
        } else {
            mt103.append("/").append(transaction.getToAccount()).append("\n");
        }

        mt103.append(transaction.getToBankName()).append("\n")
                .append(generateAddressLine()).append("\n")
                .append(generateCityCountry(transaction.getToCountryCode())).append("\n");
    }

    // Utility methods from parent class
    private String formatAmount(String amount) {
        return amount.replace(".", ",");
    }

    private boolean hasValidIBAN(String iban) {
        return iban != null && !iban.isEmpty() && !iban.equals("This country does not use IBAN");
    }

    private boolean needsIntermediaryBank(Transaction transaction) {
        try {
            var fromCountry = extractCountryCode(transaction.getFromBankSwift());
            var toCountry = extractCountryCode(transaction.getToBankSwift());
            return !fromCountry.equals(toCountry);
        } catch (StringIndexOutOfBoundsException e) {
            return true;
        }
    }

    private String extractCountryCode(String swiftCode) {
        return swiftCode.substring(4, 6);
    }

    private String getIntermediaryBankSwift(Transaction transaction) {
        try {
            var toCountry = extractCountryCode(transaction.getToBankSwift());
            return INTERMEDIARY_BANKS.getOrDefault(toCountry, "DEUTDEFFXXX");
        } catch (StringIndexOutOfBoundsException e) {
            return "DEUTDEFFXXX";
        }
    }

    private String generateAddressLine() {
        return ADDRESS_TEMPLATES[ThreadLocalRandom.current().nextInt(ADDRESS_TEMPLATES.length)];
    }

    private String generateCityCountry(String countryCode) {
        if (countryCode == null) {
            return "Unknown City, Unknown Country";
        }

        return COUNTRY_CITIES.getOrDefault(
                countryCode,
                "Financial District, " + getCountryName(countryCode)
        );
    }

    private String getCountryName(String countryCode) {
        return ADDITIONAL_COUNTRIES.getOrDefault(countryCode, "Unknown Country");
    }

    private void appendHeader(StringBuilder mt103, Transaction transaction) {
        var senderLTAddress = formatLTAddress(transaction.getFromBankSwift());
        var receiverLTAddress = formatLTAddress(transaction.getToBankSwift());
        var transactionRef = truncateToLength(transaction.getTransactionId(), 16);

        mt103.append("{1:F01").append(senderLTAddress).append("}");
        mt103.append("{2:I103").append(receiverLTAddress).append("N}");
        mt103.append("{3:{108:").append(transactionRef).append("}}");
    }

    private void appendTrailer(StringBuilder mt103) {
        mt103.append("\n{5:{MAC:").append(generateMAC())
                .append("}{CHK:").append(generateChecksum()).append("}}");
    }

    private String formatLTAddress(String bic) {
        var normalizedBIC = bic.trim().toUpperCase();

        return switch (normalizedBIC.length()) {
            case 8 -> normalizedBIC + "XXX0";
            case 11 -> normalizedBIC + "0";
            case 12 -> normalizedBIC;
            default -> normalizedBIC + "0";
        };
    }

    private String generateMAC() {
        return generateHexString(8);
    }

    private String generateChecksum() {
        return generateHexString(12);
    }

    private String generateHexString(int length) {
        var result = new StringBuilder();
        var random = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            result.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }

        return result.toString();
    }
}
