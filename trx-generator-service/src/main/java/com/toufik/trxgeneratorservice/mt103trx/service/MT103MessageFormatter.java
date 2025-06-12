package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import static com.toufik.trxgeneratorservice.mt103trx.util.MT103Constants.*;

@Service
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

    // ==============================================
    // UTILITY METHODS
    // ==============================================

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