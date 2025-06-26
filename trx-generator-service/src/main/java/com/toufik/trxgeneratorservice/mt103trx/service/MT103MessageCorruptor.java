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


@Component
@Slf4j
public class MT103MessageCorruptor {

    private final MT103MessageFormatter mt103MessageFormatter;
    private final Random random = new Random();

    public MT103MessageCorruptor(@Qualifier("MT103MessageFormatter") MT103MessageFormatter mt103MessageFormatter) {
        this.mt103MessageFormatter = mt103MessageFormatter;
    }


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

        mt103.append("{1:F01").append(formatLTAddress(transaction.getFromBankSwift())).append("}");
        mt103.append("{2:I103").append(formatLTAddress(transaction.getToBankSwift())).append("N}");
        mt103.append("{3:{108:").append(transactionRef).append("}}");

        mt103.append("\n{4:\n");

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
        String[] invalidBICs = {
                "INVALID",
                "TOOLONGBICCODE123456",
                "BANK123@",
                "123BANK",
                "",
                "BANK-US",
                "BANK US33"
        };

        String invalidFromBIC = invalidBICs[random.nextInt(invalidBICs.length)];
        String invalidToBIC = invalidBICs[random.nextInt(invalidBICs.length)];
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

        String[] invalidAmounts = {
                "",
                "INVALID",
                "123.45.67",
                "12345.123",
                "-1000,00",
                "0",
                "999999999999999.99",
                "12,34.56",
                "ABC123"
        };

        String invalidAmount = invalidAmounts[random.nextInt(invalidAmounts.length)];
        mt103.append(":32A:").append(valueDate).append(transaction.getCurrency()).append(invalidAmount).append("\n");

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