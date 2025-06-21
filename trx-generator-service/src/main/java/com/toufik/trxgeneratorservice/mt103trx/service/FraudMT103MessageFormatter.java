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

    @Override
    public String formatToMT103(Transaction transaction) {
        var mt103 = new StringBuilder();
        var transactionRef = truncateToLength(transaction.getTransactionId(), 16);

        appendHeader(mt103, transaction);
        appendMessageTextWithFraudInfo(mt103, transaction, transactionRef);
        appendTrailer(mt103);

        return mt103.toString();
    }

    private void appendMessageTextWithFraudInfo(StringBuilder mt103, Transaction transaction, String transactionRef) {
        mt103.append("\n{4:\n");

        appendMandatoryFields(mt103, transaction, transactionRef);
        appendAccountingFields(mt103, transaction);
        appendPartyFields(mt103, transaction);
        appendFraudSpecificOptionalFields(mt103, transaction);

        mt103.append("}");
    }

    private void appendFraudSpecificOptionalFields(StringBuilder mt103, Transaction transaction) {
        var remittanceInfo = buildCharacteristicBasedRemittance(transaction);

        mt103.append(":70:").append(remittanceInfo).append("\n")
                .append(":72:/INS/").append(transaction.getFromBankSwift()).append("\n");
    }

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

    private void appendAccountingFields(StringBuilder mt103, Transaction transaction) {
        var formattedAmount = formatAmount(transaction.getAmount().toString());
        var chargeBearer = transaction.isCrossBorder() ? "SHA" : "OUR";

        mt103.append(":33B:").append(transaction.getCurrency())
                .append(formattedAmount).append("\n")
                .append(":71A:").append(chargeBearer).append("\n");
    }

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