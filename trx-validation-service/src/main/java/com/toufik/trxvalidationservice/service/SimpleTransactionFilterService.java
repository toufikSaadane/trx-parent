package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SimpleTransactionFilterService {

    private static final String VALID_TRANSACTIONS_TOPIC = "transaction_alert";

    // Enhanced regex patterns for validation
    private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+([,.]\\d{1,2})?$");
    private static final Pattern FIELD_STRUCTURE_PATTERN = Pattern.compile("^:[0-9]{2}[A-Z]?:");

    @Autowired
    private KafkaTemplate<String, TransactionWithMT103Event> kafkaTemplate;

    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong validForwarded = new AtomicLong(0);
    private final AtomicLong invalidFiltered = new AtomicLong(0);

    @KafkaListener(topics = "transaction_generator", groupId = "simple-filter-group")
    public void process(TransactionWithMT103Event event) {
        String transactionId = event.getTransaction().getTransactionId();
        long processed = totalProcessed.incrementAndGet();

        try {
            String validationResult = validateMT103(event.getMt103Content());

            if (validationResult == null) {
                // Valid transaction
                kafkaTemplate.send(VALID_TRANSACTIONS_TOPIC, transactionId, event);
                long valid = validForwarded.incrementAndGet();
                log.info("✓ Forwarded: {} (Valid: {}/{})", transactionId, valid, processed);
            } else {
                // Invalid transaction
                long invalid = invalidFiltered.incrementAndGet();
                log.warn("✗ Filtered out: {} - Reason: {} (Invalid: {}/{})",
                        transactionId, validationResult, invalid, processed);
            }

            if (processed % 100 == 0) {
                logStats();
            }

        } catch (Exception e) {
            log.error("Error processing transaction {}: {}", transactionId, e.getMessage(), e);
            invalidFiltered.incrementAndGet();
        }
    }

    /**
     * Comprehensive MT103 validation
     * @param content MT103 message content
     * @return null if valid, error message if invalid
     */
    private String validateMT103(String content) {
        if (content == null || content.isBlank()) {
            return "Content is null or blank";
        }

        // 1. Check for mandatory fields
        String mandatoryCheck = checkMandatoryFields(content);
        if (mandatoryCheck != null) return mandatoryCheck;

        // 2. Check for required header blocks
        String headerCheck = checkRequiredHeaders(content);
        if (headerCheck != null) return headerCheck;

        // 3. Check field structure integrity
        String structureCheck = checkFieldStructure(content);
        if (structureCheck != null) return structureCheck;

        // 4. Check message completeness
        String completenessCheck = checkMessageCompleteness(content);
        if (completenessCheck != null) return completenessCheck;

        // 5. Check for invalid characters
        String charCheck = checkInvalidCharacters(content);
        if (charCheck != null) return charCheck;

        // 6. Validate BIC codes
        String bicCheck = validateBICCodes(content);
        if (bicCheck != null) return bicCheck;

        // 7. Validate date format
        String dateCheck = validateDateFormat(content);
        if (dateCheck != null) return dateCheck;

        // 8. Validate amount format
        String amountCheck = validateAmountFormat(content);
        if (amountCheck != null) return amountCheck;

        return null; // Valid
    }

    private String checkMandatoryFields(String content) {
        String[] mandatoryFields = {":20:", ":23B:", ":32A:"};
        for (String field : mandatoryFields) {
            if (!content.contains(field)) {
                return "Missing mandatory field: " + field;
            }
        }
        return null;
    }

    private String checkRequiredHeaders(String content) {
        String[] requiredHeaders = {"{1:", "{2:", "{3:"};
        for (String header : requiredHeaders) {
            if (!content.contains(header)) {
                return "Missing required header block: " + header;
            }
        }
        return null;
    }

    private String checkFieldStructure(String content) {
        // Check for double colons (invalid separator)
        if (content.contains("::")) {
            return "Invalid field structure: double colons found";
        }

        // Check for semicolon corruption in field tags
        String[] lines = content.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^[0-9]{2}[A-Z]?;.*")) {
                return "Invalid field structure: semicolon instead of colon in field tag";
            }
            // Check for missing colons in field tags
            if (line.matches("^[0-9]{2}[A-Z]?[^:].*") && line.length() > 3) {
                return "Invalid field structure: missing colon in field tag";
            }
        }
        return null;
    }

    private String checkMessageCompleteness(String content) {
        // Check for trailer block and proper message ending
        if (!content.contains("{5:")) {
            return "Incomplete message: missing trailer block {5:";
        }
        if (!content.trim().endsWith("}")) {
            return "Incomplete message: does not end with closing brace";
        }
        return null;
    }

    private String checkInvalidCharacters(String content) {
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            // Check for non-printable control characters (ASCII < 32)
            // Allow only newline (10), carriage return (13), and tab (9)
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                return String.format("Invalid control character found: ASCII code %d at position %d", (int) c, i);
            }
        }
        return null;
    }

    private String validateBICCodes(String content) {
        String[] bicFields = {":52A:", ":57A:", ":59:"};

        for (String field : bicFields) {
            int index = content.indexOf(field);
            if (index != -1) {
                try {
                    String fieldContent = content.substring(index + field.length());
                    int nextFieldIndex = fieldContent.indexOf("\n:");
                    if (nextFieldIndex > 0) {
                        fieldContent = fieldContent.substring(0, nextFieldIndex);
                    }

                    String[] words = fieldContent.trim().split("\\s+");
                    if (words.length > 0) {
                        String potentialBIC = words[0].replaceAll("[^A-Z0-9]", "");

                        if (potentialBIC.length() >= 4) { // Potential BIC found
                            String bicValidation = validateSingleBIC(potentialBIC);
                            if (bicValidation != null) {
                                return "Invalid BIC in field " + field + ": " + bicValidation;
                            }
                        }
                    }
                } catch (Exception e) {
                    return "Error parsing BIC in field " + field + ": " + e.getMessage();
                }
            }
        }
        return null;
    }

    private String validateSingleBIC(String bic) {
        if (bic.length() < 8) {
            return "BIC too short (minimum 8 characters): " + bic;
        }
        if (bic.length() > 11) {
            return "BIC too long (maximum 11 characters): " + bic;
        }
        if (Character.isDigit(bic.charAt(0))) {
            return "BIC cannot start with a digit: " + bic;
        }
        if (!BIC_PATTERN.matcher(bic).matches()) {
            return "BIC format invalid: " + bic;
        }
        return null;
    }

    private String validateDateFormat(String content) {
        int index = content.indexOf(":32A:");
        if (index == -1) {
            return null; // Field not found, already checked in mandatory fields
        }

        try {
            String fieldContent = content.substring(index + 5).trim();
            int nextFieldIndex = fieldContent.indexOf("\n:");
            if (nextFieldIndex > 0) {
                fieldContent = fieldContent.substring(0, nextFieldIndex);
            }

            if (fieldContent.length() < 6) {
                return "Date field too short in :32A:";
            }

            String dateStr = fieldContent.substring(0, 6);

            if (!DATE_PATTERN.matcher(dateStr).matches()) {
                return "Invalid date format in :32A: (expected YYMMDD): " + dateStr;
            }

            // Validate date logic
            try {
                int year = Integer.parseInt(dateStr.substring(0, 2));
                int month = Integer.parseInt(dateStr.substring(2, 4));
                int day = Integer.parseInt(dateStr.substring(4, 6));

                if (month < 1 || month > 12) {
                    return "Invalid month in date: " + month;
                }
                if (day < 1 || day > 31) {
                    return "Invalid day in date: " + day;
                }
                // Additional validation for specific months could be added here
                if (month == 2 && day > 29) {
                    return "Invalid day for February: " + day;
                }
                if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                    return "Invalid day for month " + month + ": " + day;
                }
            } catch (NumberFormatException e) {
                return "Non-numeric characters in date: " + dateStr;
            }
        } catch (Exception e) {
            return "Error parsing date in :32A: " + e.getMessage();
        }

        return null;
    }

    private String validateAmountFormat(String content) {
        int index = content.indexOf(":32A:");
        if (index == -1) {
            return null; // Field not found, already checked in mandatory fields
        }

        try {
            String fieldContent = content.substring(index + 5).trim();
            int nextFieldIndex = fieldContent.indexOf("\n:");
            if (nextFieldIndex > 0) {
                fieldContent = fieldContent.substring(0, nextFieldIndex);
            }

            if (fieldContent.length() <= 9) { // Date(6) + Currency(3) = 9 minimum
                return "Amount field too short in :32A:";
            }

            String amountPart = fieldContent.substring(9).trim();
            if (amountPart.isEmpty()) {
                return "Empty amount in :32A:";
            }

            // Remove non-numeric characters except comma, dot, and minus
            String cleanAmount = amountPart.replaceAll("[^0-9,.-]", "");

            if (cleanAmount.startsWith("-")) {
                return "Negative amount not allowed: " + amountPart;
            }

            // Check for multiple decimal separators
            long separatorCount = cleanAmount.chars()
                    .filter(c -> c == ',' || c == '.')
                    .count();

            if (separatorCount > 1) {
                return "Multiple decimal separators in amount: " + amountPart;
            }

            if (!AMOUNT_PATTERN.matcher(cleanAmount).matches()) {
                return "Invalid amount format: " + amountPart;
            }

        } catch (Exception e) {
            return "Error parsing amount in :32A: " + e.getMessage();
        }

        return null;
    }

    private void logStats() {
        long total = totalProcessed.get();
        long valid = validForwarded.get();
        long invalid = invalidFiltered.get();
        double validRate = total > 0 ? (double) valid / total * 100 : 0.0;
        double invalidRate = total > 0 ? (double) invalid / total * 100 : 0.0;

        log.info("=== FILTER STATS ===");
        log.info("Total Processed: {}", total);
        log.info("Valid Forwarded: {} ({:.1f}%)", valid, validRate);
        log.info("Invalid Filtered: {} ({:.1f}%)", invalid, invalidRate);
        log.info("===================");
    }
}