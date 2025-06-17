server:
port: 3002

spring:
kafka:
bootstrap-servers: localhost:9092
producer:
key-serializer: org.apache.kafka.common.serialization.StringSerializer
value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
properties:
spring.json.add.type.headers: false

mt940:
topic: "transaction_generator"
output-dir: output

mt103:
file:
directory: ./trx-generator-service/src/main/resources/mt103-transactions

transaction:
generator:
invalid-data:
enabled: true
invalid-format-percentage: 20  # 20% of transactions will have format errors
        ====
        package com.toufik.trxgeneratorservice.mt103trx.service;

import java.math.BigDecimal;
import java.util.Random;

public enum AmountGenerationStrategy {

    /**
     * Normal distribution with realistic transaction patterns
     * 50% small amounts (10-1000), 30% medium (1000-10000), 20% large (10000-100000)
     */
    NORMAL {
        @Override
        public BigDecimal generateAmount(Random random) {
            double randomValue = random.nextDouble();
            double amount;

            if (randomValue < 0.5) {
                // 50% - Small amounts: 10-1000
                amount = 10.0 + (random.nextDouble() * 990.0);
            } else if (randomValue < 0.8) {
                // 30% - Medium amounts: 1000-10000
                amount = 1000.0 + (random.nextDouble() * 9000.0);
            } else {
                // 20% - Large amounts: 10000-100000
                amount = 10000.0 + (random.nextDouble() * 90000.0);
            }

            return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
        }
    },

    /**
     * Simple normal distribution for basic transaction generation
     * Range: 100-5100
     */
    SIMPLE_NORMAL {
        @Override
        public BigDecimal generateAmount(Random random) {
            double amount = 100.0 + (random.nextDouble() * 5000.0);
            return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
        }
    };

    /**
     * Generate amount based on the specific strategy
     */
    public abstract BigDecimal generateAmount(Random random);
}
===
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Service
@Slf4j
public class BankDataService {

    private List<BankInfo> banks = new ArrayList<>();
    private Random random = new Random();

    @PostConstruct
    public void loadBanksFromCsv() {
        try {
            ClassPathResource resource = new ClassPathResource("banks.csv");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean isHeader = true;

                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }

                    String[] fields = line.split(",");
                    if (fields.length >= 5) {
                        String ibanPrefix = fields.length > 5 ? fields[5].trim() : "";
                        String ibanLengthStr = fields.length > 6 ? fields[6].trim() : "";

                        Integer ibanLength = null;
                        if (!ibanLengthStr.isEmpty()) {
                            try {
                                ibanLength = Integer.parseInt(ibanLengthStr);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid IBAN length for {}: {}", fields[0], ibanLengthStr);
                            }
                        }

                        BankInfo bank = new BankInfo(
                                fields[0].trim(), // swiftCode
                                fields[1].trim(), // countryCode
                                fields[2].trim(), // bankName
                                fields[3].trim(), // routingNumber
                                fields[4].trim(), // currencyCode
                                ibanPrefix.isEmpty() ? fields[1].trim() : ibanPrefix, // use CSV prefix or country code
                                ibanLength // use CSV length or null
                        );
                        banks.add(bank);
                    }
                }
            }
            log.info("Loaded {} banks", banks.size());
        } catch (Exception e) {
            log.error("Error loading banks: {}", e.getMessage());
            throw new RuntimeException("Failed to load bank data", e);
        }
    }

    public BankInfo getRandomBank() {
        return banks.get(random.nextInt(banks.size()));
    }

    public String generateIBAN(BankInfo bank, String accountNumber) {
        // Check if country uses IBAN based on CSV data
        if (bank.getIbanLength() == null || bank.getIbanLength() == 0) {
            return "This country does not use IBAN";
        }

        String countryCode = bank.getIbanPrefix();
        String bankCode = bank.getRoutingNumber().replaceAll("[^0-9]", "");
        if (bankCode.length() > 8) bankCode = bankCode.substring(0, 8);

        String account = padAccount(accountNumber, bank.getIbanLength(), bankCode);

        // Calculate check digits
        String temp = bankCode + account + countryCode + "00";
        String checkDigits = String.format("%02d", 98 - mod97(temp));

        return countryCode + checkDigits + bankCode + account;
    }

    private String padAccount(String accountNumber, int ibanLength, String bankCode) {
        int neededLength = ibanLength - 4 - bankCode.length(); // 4 = country(2) + check(2)
        if (neededLength <= 0) neededLength = 10; // fallback

        long accountNum = Math.abs(accountNumber.hashCode()) % (long)Math.pow(10, neededLength);
        return String.format("%0" + neededLength + "d", accountNum);
    }

    private int mod97(String input) {
        StringBuilder numeric = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(c - 'A' + 10);
            } else {
                numeric.append(c);
            }
        }

        int remainder = 0;
        for (char digit : numeric.toString().toCharArray()) {
            remainder = (remainder * 10 + Character.getNumericValue(digit)) % 97;
        }
        return remainder;
    }

    /**
     * Returns all available banks in the system
     * @return List of all bank information
     */
    public List<BankInfo> getAllBanks() {
        return new ArrayList<>(banks);
    }
}
===
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
public abstract class BaseTransactionFactory {

    @Autowired
    protected BankDataService bankDataService;

    protected final Random random = new Random();
    protected final String[] transactionStatuses = {"PENDING", "PROCESSING"};

    /**
     * Creates a base transaction with standard generation logic
     */
    protected Transaction createBaseTransaction() {
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
                generateRandomAmount(),
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

    protected BankInfo getDistinctToBank(BankInfo fromBank) {
        BankInfo toBank = bankDataService.getRandomBank();
        int attempts = 0;

        while (fromBank.getSwiftCode().equals(toBank.getSwiftCode()) && attempts < 10) {
            toBank = bankDataService.getRandomBank();
            attempts++;
        }

        return toBank;
    }

    protected String generateIBANForBank(BankInfo bank, String accountNumber) {
        try {
            String iban = bankDataService.generateIBAN(bank, accountNumber);
            if (iban != null) {
                log.debug("Generated IBAN {} for bank {}", iban, bank.getSwiftCode());
                return iban;
            } else {
                log.warn("Could not generate IBAN for bank {} ({}), country: {}",
                        bank.getBankName(), bank.getSwiftCode(), bank.getCountryCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error generating IBAN for bank {}: {}", bank.getSwiftCode(), e.getMessage());
            return null;
        }
    }

    protected String determineCurrency(BankInfo fromBank, BankInfo toBank) {
        if (isEuropeanBank(fromBank) && isEuropeanBank(toBank)) {
            return "EUR";
        } else if (isUSBank(fromBank) || isUSBank(toBank)) {
            return "USD";
        }
        return fromBank.getCurrencyCode();
    }

    protected boolean isEuropeanBank(BankInfo bank) {
        return bank.getCountryCode().matches("DE|FR|IT|ES|NL|BE|AT|PT|IE|FI|LU");
    }

    protected boolean isUSBank(BankInfo bank) {
        return "US".equals(bank.getCountryCode());
    }

    protected String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();
        int length = random.nextDouble() < 0.7 ? 12 : 10;

        for (int i = 0; i < length; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    /**
     * Abstract method to be implemented by subclasses for different amount generation strategies
     */
    protected abstract BigDecimal generateRandomAmount();
}
=====
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
=====
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
=====
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

import com.toufik.trxgeneratorservice.mt103trx.model.InvalidScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Factory for creating invalid transactions with corrupted MT103 messages
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionFactory {

    private final TransactionGeneratorService transactionGenerator;
    private final MT103MessageCorruptor mt103MessageCorruptor;
    private final Random random = new Random();

    // Array of all available invalid scenarios
    private final InvalidScenario[] invalidScenarios = InvalidScenario.values();

    /**
     * Creates a transaction with an invalid MT103 message based on a random scenario
     */
    public TransactionWithMT103Event createInvalidTransaction() {
        // Generate a base transaction
        Transaction transaction = transactionGenerator.generateRandomTransaction();

        // Select random invalid scenario
        InvalidScenario scenario = selectRandomScenario();

        // Generate invalid MT103 content based on scenario
        String invalidMT103Content = mt103MessageCorruptor.generateInvalidMT103(transaction, scenario);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(transaction);
        result.setMt103Content(invalidMT103Content);

        log.warn("Created invalid transaction with scenario: {} for transaction: {}",
                scenario, transaction.getTransactionId());

        return result;
    }

    private InvalidScenario selectRandomScenario() {
        return invalidScenarios[random.nextInt(invalidScenarios.length)];
    }
}
======
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating and sending invalid transactions at scheduled intervals
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionGeneratorService {

    private final InvalidTransactionFactory invalidTransactionFactory;
    private final TransactionProducer transactionProducer;

    /**
     * Generates invalid transactions every 15 seconds
     */
    @Scheduled(fixedRate = 15000)
    public void generateAndSendInvalidTransaction() {
        try {
            TransactionWithMT103Event invalidTransactionEvent = invalidTransactionFactory.createInvalidTransaction();

            logInvalidTransactionDetails(invalidTransactionEvent);

            transactionProducer.sendTransaction(invalidTransactionEvent);

            log.info("Successfully sent invalid transaction: {}",
                    invalidTransactionEvent.getTransaction().getTransactionId());

        } catch (Exception e) {
            log.error("Error generating invalid transaction: {}", e.getMessage(), e);
        }
    }


    private void logInvalidTransactionDetails(TransactionWithMT103Event invalidTransactionEvent) {
        log.warn("Generated INVALID transaction: {}",
                invalidTransactionEvent.getTransaction().getTransactionId());
        log.warn("======================= INVALID TRANSACTION =============================");
        log.warn("Invalid Transaction Details: {}", invalidTransactionEvent.getTransaction());

        String mt103Content = invalidTransactionEvent.getMt103Content();
        String preview = mt103Content.substring(0, Math.min(200, mt103Content.length()));
        log.warn("Invalid MT103 Content Preview: {}", preview);

        if (mt103Content.length() > 200) {
            log.warn("MT103 Content Length: {} characters (truncated for logging)", mt103Content.length());
        }
    }
}
======
        package com.toufik.trxgeneratorservice.mt103trx.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MT103FileService {

    private final Map<String, String> mt103FileMap = new ConcurrentHashMap<>();

    @Value("${mt103.file.directory:./mt103-transactions}")
    private String mt103Directory;


    public void saveMT103ToFile(String transactionId, String mt103Content) throws IOException {
        log.info("Attempting to save MT103 file for transaction: {}", transactionId);

        if (transactionId == null || mt103Content == null) {
            log.error("Transaction ID or MT103 content is null");
            return;
        }

        // Create directory if it doesn't exist
        Path directoryPath = Paths.get(mt103Directory);

        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
            log.info("Created directory at: {}", directoryPath.toAbsolutePath());
        }

        // Generate filename
        String fileName = "MT103_" + transactionId.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
        Path filePath = directoryPath.resolve(fileName);

        // Write MT103 content to file
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(mt103Content);
        }

        log.info("Saved MT103 file to: {}", filePath.toAbsolutePath());

        // Store in map for quick access
        mt103FileMap.put(transactionId, filePath.toString());
        log.info("Current map size: {}", mt103FileMap.size());
    }

    public String getMT103FilePath(String transactionId) {
        String path = mt103FileMap.get(transactionId);
        log.info("Retrieved path for transaction {}: {}", transactionId, path);
        return path;
    }
}
====
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.InvalidScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.service.MT103MessageFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Handles corruption of MT103 messages for testing invalid scenarios
 */
@Component
@Slf4j
public class MT103MessageCorruptor {

    private final MT103MessageFormatter mt103MessageFormatter;
    private final Random random = new Random();

    public MT103MessageCorruptor(@Qualifier("MT103MessageFormatter") MT103MessageFormatter mt103MessageFormatter) {
        this.mt103MessageFormatter = mt103MessageFormatter;
    }

    /**
     * Generates an invalid MT103 message based on the specified scenario
     */
    public String generateInvalidMT103(Transaction transaction, InvalidScenario scenario) {
        log.debug("Generating invalid MT103 for scenario: {}", scenario);

        return switch (scenario) {
            case MISSING_MANDATORY_FIELDS -> generateMT103WithMissingMandatoryFields(transaction);
            case INVALID_BIC_FORMAT -> generateMT103WithInvalidBIC(transaction);
            case INVALID_DATE_FORMAT -> generateMT103WithInvalidDate(transaction);
            case INVALID_AMOUNT_FORMAT -> generateMT103WithInvalidAmount(transaction);
            case MISSING_HEADER_BLOCKS -> generateMT103WithMissingHeaders(transaction);
            case INVALID_FIELD_STRUCTURE -> generateMT103WithInvalidFieldStructure(transaction);
            case TRUNCATED_MESSAGES -> generateTruncatedMT103(transaction);
            case INVALID_CHARACTERS -> generateMT103WithInvalidCharacters(transaction);
        };
    }

    private String generateMT103WithMissingMandatoryFields(Transaction transaction) {
        StringBuilder mt103 = new StringBuilder();
        String transactionRef = transaction.getTransactionId().substring(0, Math.min(16, transaction.getTransactionId().length()));

        // Normal header
        mt103.append("{1:F01").append(formatLTAddress(transaction.getFromBankSwift())).append("}");
        mt103.append("{2:I103").append(formatLTAddress(transaction.getToBankSwift())).append("N}");
        mt103.append("{3:{108:").append(transactionRef).append("}}");

        mt103.append("\n{4:\n");

        // Randomly remove mandatory fields (20, 23B, 32A)
        if (random.nextBoolean()) {
            // Skip :20: field
        } else {
            mt103.append(":20:").append(transactionRef).append("\n");
        }

        if (random.nextBoolean()) {
            // Skip :23B: field
        } else {
            mt103.append(":23B:CRED\n");
        }

        if (random.nextBoolean()) {
            // Skip :32A: field
        } else {
            String valueDate = transaction.getTimestamp().format(DateTimeFormatter.ofPattern("yyMMdd"));
            mt103.append(":32A:").append(valueDate).append(transaction.getCurrency())
                    .append(transaction.getAmount().toString().replace(".", ",")).append("\n");
        }

        // Add some other fields
        mt103.append(":50K:/").append(transaction.getFromAccount()).append("\n");
        mt103.append(transaction.getFromBankName()).append("\n");
        mt103.append(":59:/").append(transaction.getToAccount()).append("\n");
        mt103.append(transaction.getToBankName()).append("\n");

        mt103.append("}");
        mt103.append("\n{5:{MAC:12345678}{CHK:123456789ABC}}");

        return mt103.toString();
    }

    private String generateMT103WithInvalidBIC(Transaction transaction) {
        // Create invalid BICs
        String[] invalidBICs = {
                "INVALID", // Too short
                "TOOLONGBICCODE123456", // Too long
                "BANK123@", // Invalid characters
                "123BANK", // Starting with numbers
                "", // Empty
                "BANK-US", // Invalid format
                "BANK US33" // Space in BIC
        };

        String invalidFromBIC = invalidBICs[random.nextInt(invalidBICs.length)];
        String invalidToBIC = invalidBICs[random.nextInt(invalidBICs.length)];

        // Create transaction copy with invalid BICs
        Transaction invalidTransaction = new Transaction(
                transaction.getTransactionId(),
                transaction.getFromAccount(),
                transaction.getToAccount(),
                transaction.getAmount(),
                transaction.getCurrency(),
                invalidFromBIC,
                invalidToBIC,
                transaction.getFromBankName(),
                transaction.getToBankName(),
                transaction.getTimestamp(),
                transaction.getStatus()
        );

        return mt103MessageFormatter.formatToMT103(invalidTransaction);
    }

    private String generateMT103WithInvalidDate(Transaction transaction) {
        StringBuilder mt103 = new StringBuilder();
        String transactionRef = transaction.getTransactionId().substring(0, Math.min(16, transaction.getTransactionId().length()));

        // Normal header
        mt103.append("{1:F01").append(formatLTAddress(transaction.getFromBankSwift())).append("}");
        mt103.append("{2:I103").append(formatLTAddress(transaction.getToBankSwift())).append("N}");
        mt103.append("{3:{108:").append(transactionRef).append("}}");

        mt103.append("\n{4:\n");
        mt103.append(":20:").append(transactionRef).append("\n");
        mt103.append(":23B:CRED\n");

        // Generate invalid date formats
        String[] invalidDates = {
                "20241301", // Invalid month
                "20240230", // Invalid day
                "240101",   // Wrong year format
                "2024-01-01", // Wrong format with dashes
                "INVALID",  // Non-numeric
                "999999",   // Wrong length
                "",         // Empty
                "20241401"  // Invalid month
        };

        String invalidDate = invalidDates[random.nextInt(invalidDates.length)];
        mt103.append(":32A:").append(invalidDate).append(transaction.getCurrency())
                .append(transaction.getAmount().toString().replace(".", ",")).append("\n");

        // Add remaining fields
        mt103.append(":50K:/").append(transaction.getFromAccount()).append("\n");
        mt103.append(transaction.getFromBankName()).append("\n");
        mt103.append(":59:/").append(transaction.getToAccount()).append("\n");
        mt103.append(transaction.getToBankName()).append("\n");

        mt103.append("}");
        mt103.append("\n{5:{MAC:12345678}{CHK:123456789ABC}}");

        return mt103.toString();
    }

    private String generateMT103WithInvalidAmount(Transaction transaction) {
        StringBuilder mt103 = new StringBuilder();
        String transactionRef = transaction.getTransactionId().substring(0, Math.min(16, transaction.getTransactionId().length()));

        // Normal header
        mt103.append("{1:F01").append(formatLTAddress(transaction.getFromBankSwift())).append("}");
        mt103.append("{2:I103").append(formatLTAddress(transaction.getToBankSwift())).append("N}");
        mt103.append("{3:{108:").append(transactionRef).append("}}");

        mt103.append("\n{4:\n");
        mt103.append(":20:").append(transactionRef).append("\n");
        mt103.append(":23B:CRED\n");

        String valueDate = transaction.getTimestamp().format(DateTimeFormatter.ofPattern("yyMMdd"));

        // Generate invalid amounts
        String[] invalidAmounts = {
                "", // Empty amount
                "INVALID", // Non-numeric
                "123.45.67", // Multiple decimals
                "12345.123", // Too many decimal places
                "-1000,00", // Negative amount
                "0", // Zero amount
                "999999999999999.99", // Too large
                "12,34.56", // Mixed separators
                "ABC123"  // Mixed alphanumeric
        };

        String invalidAmount = invalidAmounts[random.nextInt(invalidAmounts.length)];
        mt103.append(":32A:").append(valueDate).append(transaction.getCurrency()).append(invalidAmount).append("\n");

        // Add remaining fields
        mt103.append(":50K:/").append(transaction.getFromAccount()).append("\n");
        mt103.append(transaction.getFromBankName()).append("\n");
        mt103.append(":59:/").append(transaction.getToAccount()).append("\n");
        mt103.append(transaction.getToBankName()).append("\n");

        mt103.append("}");
        mt103.append("\n{5:{MAC:12345678}{CHK:123456789ABC}}");

        return mt103.toString();
    }

    private String generateMT103WithMissingHeaders(Transaction transaction) {
        StringBuilder mt103 = new StringBuilder();
        String transactionRef = transaction.getTransactionId().substring(0, Math.min(16, transaction.getTransactionId().length()));

        // Randomly skip header blocks
        if (random.nextBoolean()) {
            // Skip Block 1
        } else {
            mt103.append("{1:F01").append(formatLTAddress(transaction.getFromBankSwift())).append("}");
        }

        if (random.nextBoolean()) {
            // Skip Block 2
        } else {
            mt103.append("{2:I103").append(formatLTAddress(transaction.getToBankSwift())).append("N}");
        }

        if (random.nextBoolean()) {
            // Skip Block 3
        } else {
            mt103.append("{3:{108:").append(transactionRef).append("}}");
        }

        // Always include Block 4
        mt103.append("\n{4:\n");
        mt103.append(":20:").append(transactionRef).append("\n");
        mt103.append(":23B:CRED\n");

        String valueDate = transaction.getTimestamp().format(DateTimeFormatter.ofPattern("yyMMdd"));
        mt103.append(":32A:").append(valueDate).append(transaction.getCurrency())
                .append(transaction.getAmount().toString().replace(".", ",")).append("\n");

        mt103.append(":50K:/").append(transaction.getFromAccount()).append("\n");
        mt103.append(transaction.getFromBankName()).append("\n");
        mt103.append(":59:/").append(transaction.getToAccount()).append("\n");
        mt103.append(transaction.getToBankName()).append("\n");

        mt103.append("}");

        return mt103.toString();
    }

    private String generateMT103WithInvalidFieldStructure(Transaction transaction) {
        StringBuilder mt103 = new StringBuilder();
        String transactionRef = transaction.getTransactionId().substring(0, Math.min(16, transaction.getTransactionId().length()));

        // Normal header
        mt103.append("{1:F01").append(formatLTAddress(transaction.getFromBankSwift())).append("}");
        mt103.append("{2:I103").append(formatLTAddress(transaction.getToBankSwift())).append("N}");
        mt103.append("{3:{108:").append(transactionRef).append("}}");

        mt103.append("\n{4:\n");

        // Corrupt field separators and structure
        String[] corruptedFields = {
                "20;" + transactionRef + "\n", // Wrong separator
                "::20:" + transactionRef + "\n", // Double colon
                ":20" + transactionRef + "\n", // Missing colon
                ":20::" + transactionRef + "\n", // Extra colon
                "20:" + transactionRef + "\n", // Missing first colon
                ":23B;CRED\n", // Wrong separator
                ":32A=" + transaction.getTimestamp().format(DateTimeFormatter.ofPattern("yyMMdd")) +
                        transaction.getCurrency() + transaction.getAmount().toString().replace(".", ",") + "\n" // Wrong separator
        };

        // Add some corrupted fields
        for (int i = 0; i < 3; i++) {
            mt103.append(corruptedFields[random.nextInt(corruptedFields.length)]);
        }

        mt103.append("50K:/").append(transaction.getFromAccount()).append("\n"); // Missing colon
        mt103.append(transaction.getFromBankName()).append("\n");
        mt103.append(":59/").append(transaction.getToAccount()).append("\n"); // Missing colon
        mt103.append(transaction.getToBankName()).append("\n");

        mt103.append("}");
        mt103.append("\n{5:{MAC:12345678}{CHK:123456789ABC}}");

        return mt103.toString();
    }

    private String generateTruncatedMT103(Transaction transaction) {
        // Generate normal MT103 first
        String normalMT103 = mt103MessageFormatter.formatToMT103(transaction);

        // Truncate at various points
        int[] truncationPoints = {
                normalMT103.length() / 4,      // 25%
                normalMT103.length() / 2,      // 50%
                normalMT103.length() * 3 / 4,  // 75%
                normalMT103.length() - 50      // Near end
        };

        int truncateAt = truncationPoints[random.nextInt(truncationPoints.length)];
        truncateAt = Math.max(10, Math.min(truncateAt, normalMT103.length() - 1));

        return normalMT103.substring(0, truncateAt);
    }

    private String generateMT103WithInvalidCharacters(Transaction transaction) {
        String normalMT103 = mt103MessageFormatter.formatToMT103(transaction);

        // Add invalid control characters
        char[] invalidChars = {'\0', '\u0001', '\u0002', '\u0007', '\u0008', '\u000B', '\u000C', '\u000E', '\u000F'};

        StringBuilder corruptedMT103 = new StringBuilder();
        for (int i = 0; i < normalMT103.length(); i++) {
            corruptedMT103.append(normalMT103.charAt(i));

            // 5% chance to insert invalid character
            if (random.nextDouble() < 0.05) {
                corruptedMT103.append(invalidChars[random.nextInt(invalidChars.length)]);
            }
        }

        return corruptedMT103.toString();
    }

    private String formatLTAddress(String bic) {
        if (bic == null || bic.isEmpty()) {
            return "UNKNOWNXXX0";
        }

        String normalizedBIC = bic.trim().toUpperCase();

        return switch (normalizedBIC.length()) {
            case 8 -> normalizedBIC + "XXX0";
            case 11 -> normalizedBIC + "0";
            case 12 -> normalizedBIC;
            default -> normalizedBIC + "0";
        };
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
======
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;

/**
 * Interface for transaction factory implementations
 */
public interface TransactionFactory {

    /**
     * Creates a new transaction
     * @return Transaction instance
     */
    Transaction createTransaction();
}
===
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
====
        package com.toufik.trxgeneratorservice.mt103trx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class TransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MT103FileService mt103FileService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate, MT103FileService mt103FileService) {
        this.kafkaTemplate = kafkaTemplate;
        this.mt103FileService = mt103FileService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        log.info("TransactionProducer initialized with MT103FileService: {}", mt103FileService);
    }

    public void sendTransaction(TransactionWithMT103Event transactionWithMT103Event) {
        if (transactionWithMT103Event == null) {
            log.error("Received null TransactionWithMT103Event");
            return;
        }
        try {
            // Step 1: Save MT103 to file and store in map
            mt103FileService.saveMT103ToFile(
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    transactionWithMT103Event.getMt103Content()
            );
            log.info("Saved MT103 file for transaction: {} at path: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    mt103FileService.getMT103FilePath(transactionWithMT103Event.getTransaction().getTransactionId()));

            // Step 2: Send JSON message to Kafka
            kafkaTemplate.send(
                    "transaction_generator",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    transactionWithMT103Event
            );
            log.info("Sent transaction with MT103 info to Kafka: {}",
                    transactionWithMT103Event);

        } catch (IOException e) {
            log.error("Error saving MT103 file for transaction {}: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing transaction {}: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(), e.getMessage(), e);
        }
    }
}
========