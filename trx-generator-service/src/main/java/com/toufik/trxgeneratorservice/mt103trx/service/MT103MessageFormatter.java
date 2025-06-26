package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import static com.toufik.trxgeneratorservice.mt103trx.util.MT103Constants.*;

@Service("MT103MessageFormatter")
public class MT103MessageFormatter {


    public String formatToMT103(Transaction transaction) {
        var mt103 = new StringBuilder();
        var transactionRef = truncateToLength(transaction.getTransactionId(), 16);

        appendHeader(mt103, transaction);
        appendMessageText(mt103, transaction, transactionRef);
        appendTrailer(mt103);

        return mt103.toString();
    }

    private void appendHeader(StringBuilder mt103, Transaction transaction) {
        var senderLTAddress = formatLTAddress(transaction.getFromBankSwift());
        var receiverLTAddress = formatLTAddress(transaction.getToBankSwift());
        var transactionRef = truncateToLength(transaction.getTransactionId(), 16);

        mt103.append("{1:F01").append(senderLTAddress).append("}");
        mt103.append("{2:I103").append(receiverLTAddress).append("N}");
        mt103.append("{3:{108:").append(transactionRef).append("}}");
    }

    private void appendMessageText(StringBuilder mt103, Transaction transaction, String transactionRef) {
        mt103.append("\n{4:\n");

        appendMandatoryFields(mt103, transaction, transactionRef);
        appendAccountingFields(mt103, transaction);
        appendPartyFields(mt103, transaction);
        appendOptionalFields(mt103, transaction);

        mt103.append("}");
    }

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

    private void appendOptionalFields(StringBuilder mt103, Transaction transaction) {
        var remittanceInfo = buildRemittanceInfo(transaction);

        mt103.append(":70:").append(remittanceInfo).append("\n")
                .append(":72:/INS/").append(transaction.getFromBankSwift()).append("\n");
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

    private String formatAmount(String amount) {
        return amount.replace(".", ",");
    }

    private String truncateToLength(String input, int maxLength) {
        if (input == null) return "";
        return input.length() > maxLength ? input.substring(0, maxLength) : input;
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

    private String buildRemittanceInfo(Transaction transaction) {
        var shortTransactionId = truncateToLength(transaction.getTransactionId(), 8);
        var remittance = new StringBuilder()
                .append("Payment for services - TXN ID: ").append(shortTransactionId);

        if (transaction.isCrossBorder()) {
            remittance.append(" - Cross-border transfer");
        }

        return remittance.toString();
    }

    private boolean hasValidIBAN(String iban) {
        return iban != null && !iban.isEmpty() && !iban.equals("This country does not use IBAN");
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