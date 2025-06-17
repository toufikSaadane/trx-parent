package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DebugTransactionService {

    private final MT103FieldValidator fieldValidator;

    @Autowired
    public DebugTransactionService(MT103FieldValidator fieldValidator) {
        this.fieldValidator = fieldValidator;
    }

    public void debugTransaction(TransactionWithMT103Event transactionEvent) {
        log.info("=== DEBUG TRANSACTION {} ===", transactionEvent.getTransaction().getTransactionId());

        String mt103Content = transactionEvent.getMt103Content();
        log.info("MT103 Content: {}", mt103Content);

        // Check mandatory fields
        List<String> mandatoryErrors = fieldValidator.validateMandatoryFields(mt103Content);
        log.info("Mandatory field errors ({}): {}", mandatoryErrors.size(), mandatoryErrors);

        // Check BIC format
        List<String> bicErrors = fieldValidator.validateBICFormat(mt103Content);
        log.info("BIC format errors ({}): {}", bicErrors.size(), bicErrors);

        // Check date format
        List<String> dateErrors = fieldValidator.validateDateFormat(mt103Content);
        log.info("Date format errors ({}): {}", dateErrors.size(), dateErrors);

        // Check amount format
        List<String> amountErrors = fieldValidator.validateAmountFormat(mt103Content);
        log.info("Amount format errors ({}): {}", amountErrors.size(), amountErrors);

        int totalErrors = mandatoryErrors.size() + bicErrors.size() + dateErrors.size() + amountErrors.size();
        log.info("TOTAL ERRORS: {}", totalErrors);
        log.info("TRANSACTION WILL BE CLASSIFIED AS: {}", totalErrors == 0 ? "VALID" : "INVALID");
        log.info("=== END DEBUG ===");
    }
}
====
        package com.toufik.trxvalidationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Primary
@Slf4j
public class LenientMT103FieldValidator {

    private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+([,.]\\d{1,2})?$");

    public List<String> validateMandatoryFields(String mt103Content) {
        List<String> errors = new ArrayList<>();

        if (mt103Content == null || mt103Content.trim().isEmpty()) {
            errors.add("MT103 content is null or empty");
            return errors;
        }

        log.debug("Validating mandatory fields for MT103 content: {}", mt103Content.substring(0, Math.min(100, mt103Content.length())));

        // More lenient check - look for the field patterns anywhere in the content
        if (!containsField(mt103Content, "20")) {
            errors.add("Missing mandatory field :20: (Transaction Reference)");
        }

        if (!containsField(mt103Content, "23B")) {
            errors.add("Missing mandatory field :23B: (Bank Operation Code)");
        }

        if (!containsField(mt103Content, "32A")) {
            errors.add("Missing mandatory field :32A: (Value Date/Currency/Amount)");
        }

        log.debug("Mandatory fields validation completed with {} errors", errors.size());
        return errors;
    }

    public List<String> validateBICFormat(String mt103Content) {
        List<String> errors = new ArrayList<>();

        log.debug("Validating BIC format - being more lenient");

        // Extract BIC codes from fields 52A, 57A, 59 - but be more lenient
        List<String> bicFields = extractBICFields(mt103Content);

        for (String bic : bicFields) {
            // More lenient BIC validation
            if (bic.length() < 8 || bic.length() > 11) {
                log.warn("BIC length issue but continuing: {} (length: {})", bic, bic.length());
                // Don't add as error - just warn
                continue;
            }

            // Only check if it starts with a digit - that's a clear error
            if (Character.isDigit(bic.charAt(0))) {
                errors.add("Invalid BIC format: " + bic + " (cannot start with digit)");
                continue;
            }

            // More lenient pattern matching - don't be too strict
            if (bic.length() >= 8 && !bic.matches("^[A-Z0-9]+$")) {
                log.warn("BIC contains non-alphanumeric characters but continuing: {}", bic);
                // Don't add as error for now
            }
        }

        log.debug("BIC format validation completed with {} errors", errors.size());
        return errors;
    }

    public List<String> validateDateFormat(String mt103Content) {
        List<String> errors = new ArrayList<>();

        log.debug("Validating date format - being more lenient");

        // Extract dates from field 32A - but be more lenient
        List<String> dates = extractDatesFromField32A(mt103Content);

        for (String date : dates) {
            if (date.length() != 6) {
                errors.add("Invalid date length: " + date + " (must be 6 digits YYMMDD)");
                continue;
            }

            if (!date.matches("^\\d{6}$")) {
                errors.add("Invalid date format: " + date + " (must be numeric YYMMDD)");
                continue;
            }

            // Basic date validation - be more lenient
            if (!isBasicValidDate(date)) {
                log.warn("Date might be invalid but continuing: {}", date);
                // Don't add as error - just warn
            }
        }

        log.debug("Date format validation completed with {} errors", errors.size());
        return errors;
    }

    public List<String> validateAmountFormat(String mt103Content) {
        List<String> errors = new ArrayList<>();

        log.debug("Validating amount format - being more lenient");

        // Extract amounts from field 32A - but be more lenient
        List<String> amounts = extractAmountsFromField32A(mt103Content);

        for (String amount : amounts) {
            if (amount.trim().isEmpty()) {
                errors.add("Amount is empty");
                continue;
            }

            if (amount.startsWith("-")) {
                errors.add("Invalid amount: " + amount + " (negative values not allowed)");
                continue;
            }

            // Be more lenient with amount format
            if (!amount.matches("^\\d+([,.]\\d{1,4})?$")) {
                log.warn("Amount format might be unusual but continuing: {}", amount);
                // Don't add as error - just warn
            }
        }

        log.debug("Amount format validation completed with {} errors", errors.size());
        return errors;
    }

    private boolean containsField(String content, String fieldNumber) {
        return content.contains(":" + fieldNumber + ":") ||
                content.contains("{" + fieldNumber + ":") ||
                content.contains(fieldNumber + ":");
    }

    private List<String> extractBICFields(String mt103Content) {
        List<String> bics = new ArrayList<>();

        // Be more flexible in extracting BIC codes
        String[] patterns = {":52A:", ":57A:", ":59:", "52A:", "57A:", "59:"};

        for (String pattern : patterns) {
            int index = mt103Content.indexOf(pattern);
            if (index != -1) {
                int start = index + pattern.length();
                // Look for the next field or end of content
                int end = findNextFieldOrEnd(mt103Content, start);

                String fieldContent = mt103Content.substring(start, end).trim();
                // Extract first word that could be a BIC
                String[] words = fieldContent.split("\\s+");
                if (words.length > 0 && words[0].length() >= 8 && words[0].length() <= 11) {
                    String potentialBIC = words[0].replaceAll("[^A-Z0-9]", "");
                    if (potentialBIC.length() >= 8) {
                        bics.add(potentialBIC);
                    }
                }
            }
        }

        return bics;
    }

    private List<String> extractDatesFromField32A(String mt103Content) {
        List<String> dates = new ArrayList<>();

        String[] patterns = {":32A:", "32A:"};

        for (String pattern : patterns) {
            int index = mt103Content.indexOf(pattern);
            if (index != -1) {
                int start = index + pattern.length();
                int end = findNextFieldOrEnd(mt103Content, start);

                String fieldContent = mt103Content.substring(start, end).trim();
                // Look for 6-digit date at the beginning
                if (fieldContent.length() >= 6) {
                    String potentialDate = fieldContent.substring(0, 6);
                    if (potentialDate.matches("\\d{6}")) {
                        dates.add(potentialDate);
                    }
                }
            }
        }

        return dates;
    }

    private List<String> extractAmountsFromField32A(String mt103Content) {
        List<String> amounts = new ArrayList<>();

        String[] patterns = {":32A:", "32A:"};

        for (String pattern : patterns) {
            int index = mt103Content.indexOf(pattern);
            if (index != -1) {
                int start = index + pattern.length();
                int end = findNextFieldOrEnd(mt103Content, start);

                String fieldContent = mt103Content.substring(start, end).trim();
                // Format: YYMMDDCCCAMOUNT - extract amount part
                if (fieldContent.length() > 9) {
                    String amount = fieldContent.substring(9).replaceAll("[^0-9,.]", "");
                    if (!amount.isEmpty()) {
                        amounts.add(amount);
                    }
                }
            }
        }

        return amounts;
    }

    private int findNextFieldOrEnd(String content, int start) {
        int nextField = content.length();

        // Look for next field pattern
        for (int i = start; i < content.length() - 1; i++) {
            if (content.charAt(i) == ':' && Character.isDigit(content.charAt(i + 1))) {
                nextField = i;
                break;
            }
        }

        return nextField;
    }

    private boolean isBasicValidDate(String date) {
        try {
            int month = Integer.parseInt(date.substring(2, 4));
            int day = Integer.parseInt(date.substring(4, 6));

            return month >= 1 && month <= 12 && day >= 1 && day <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
====
        package com.toufik.trxvalidationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MT103FieldValidator {

    private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+([,.]\\d{1,2})?$");

    public List<String> validateMandatoryFields(String mt103Content) {
        List<String> errors = new ArrayList<>();

        if (mt103Content == null || mt103Content.isEmpty()) {
            errors.add("MT103 content is null or empty");
            return errors;
        }

        log.debug("Validating mandatory fields for MT103 content");

        // Check for mandatory fields: 20, 23B, 32A
        if (!mt103Content.contains(":20:")) {
            errors.add("Missing mandatory field :20: (Transaction Reference)");
        }

        if (!mt103Content.contains(":23B:")) {
            errors.add("Missing mandatory field :23B: (Bank Operation Code)");
        }

        if (!mt103Content.contains(":32A:")) {
            errors.add("Missing mandatory field :32A: (Value Date/Currency/Amount)");
        }

        log.debug("Mandatory fields validation completed with {} errors", errors.size());
        return errors;
    }

    public List<String> validateBICFormat(String mt103Content) {
        List<String> errors = new ArrayList<>();

        log.debug("Validating BIC format");

        // Extract BIC codes from fields 52A, 57A, 59
        List<String> bicFields = extractBICFields(mt103Content);

        for (String bic : bicFields) {
            if (bic.length() < 8 || bic.length() > 11) {
                errors.add("Invalid BIC length: " + bic + " (must be 8-11 characters)");
                continue;
            }

            // Check if first character is a digit - BIC cannot start with digit
            if (Character.isDigit(bic.charAt(0))) {
                errors.add("Invalid BIC format: " + bic + " (cannot start with digit)");
                continue;
            }

            if (!BIC_PATTERN.matcher(bic).matches()) {
                errors.add("Invalid BIC format: " + bic + " (invalid characters or structure)");
            }
        }

        log.debug("BIC format validation completed with {} errors", errors.size());
        return errors;
    }

    public List<String> validateDateFormat(String mt103Content) {
        List<String> errors = new ArrayList<>();

        log.debug("Validating date format");

        // Extract dates from field 32A
        List<String> dates = extractDatesFromField32A(mt103Content);

        for (String date : dates) {
            if (!DATE_PATTERN.matcher(date).matches()) {
                errors.add("Invalid date format: " + date + " (must be YYMMDD)");
                continue;
            }

            // Additional date logic validation
            if (!isValidDate(date)) {
                errors.add("Invalid date value: " + date);
            }
        }

        log.debug("Date format validation completed with {} errors", errors.size());
        return errors;
    }

    public List<String> validateAmountFormat(String mt103Content) {
        List<String> errors = new ArrayList<>();

        log.debug("Validating amount format");

        // Extract amounts from field 32A
        List<String> amounts = extractAmountsFromField32A(mt103Content);

        for (String amount : amounts) {
            if (amount.startsWith("-")) {
                errors.add("Invalid amount: " + amount + " (negative values not allowed)");
                continue;
            }

            if (amount.contains("..") || amount.contains(",,")) {
                errors.add("Invalid amount format: " + amount + " (multiple decimal separators)");
                continue;
            }

            if (!AMOUNT_PATTERN.matcher(amount).matches()) {
                errors.add("Invalid amount format: " + amount + " (invalid characters or structure)");
            }
        }

        log.debug("Amount format validation completed with {} errors", errors.size());
        return errors;
    }

    private List<String> extractBICFields(String mt103Content) {
        List<String> bics = new ArrayList<>();

        // Extract from :52A:, :57A:, :59: fields
        String[] patterns = {":52A:", ":57A:", ":59:"};

        for (String pattern : patterns) {
            int index = mt103Content.indexOf(pattern);
            if (index != -1) {
                int start = index + pattern.length();
                int end = mt103Content.indexOf("\n", start);
                if (end == -1) end = mt103Content.length();

                String fieldContent = mt103Content.substring(start, end).trim();
                if (!fieldContent.isEmpty() && fieldContent.length() >= 8) {
                    // Extract potential BIC code (first 8-11 characters)
                    String potentialBIC = fieldContent.substring(0, Math.min(11, fieldContent.length()));
                    // Remove any non-alphanumeric characters except those allowed in BIC
                    potentialBIC = potentialBIC.replaceAll("[^A-Z0-9]", "");
                    if (potentialBIC.length() >= 8) {
                        bics.add(potentialBIC);
                    }
                }
            }
        }

        return bics;
    }

    private List<String> extractDatesFromField32A(String mt103Content) {
        List<String> dates = new ArrayList<>();

        int index = mt103Content.indexOf(":32A:");
        if (index != -1) {
            int start = index + 5;
            int end = mt103Content.indexOf("\n", start);
            if (end == -1) end = mt103Content.length();

            String fieldContent = mt103Content.substring(start, end).trim();
            if (fieldContent.length() >= 6) {
                dates.add(fieldContent.substring(0, 6));
            }
        }

        return dates;
    }

    private List<String> extractAmountsFromField32A(String mt103Content) {
        List<String> amounts = new ArrayList<>();

        int index = mt103Content.indexOf(":32A:");
        if (index != -1) {
            int start = index + 5;
            int end = mt103Content.indexOf("\n", start);
            if (end == -1) end = mt103Content.length();

            String fieldContent = mt103Content.substring(start, end).trim();
            // Format: YYMMDDCCCAMOUNT
            if (fieldContent.length() > 9) {
                String amount = fieldContent.substring(9);
                amounts.add(amount);
            }
        }

        return amounts;
    }

    private boolean isValidDate(String date) {
        try {
            int year = Integer.parseInt(date.substring(0, 2));
            int month = Integer.parseInt(date.substring(2, 4));
            int day = Integer.parseInt(date.substring(4, 6));

            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;

            // Additional month-day validation
            if (month == 2 && day > 29) return false;
            if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
=====
        package com.toufik.trxvalidationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MT103StructureValidator {

    private static final Pattern FIELD_PATTERN = Pattern.compile(":[0-9]{2}[A-Z]?:");

    public List<String> validateMessageStructure(String mt103Content) {
        List<String> errors = new ArrayList<>();

        if (mt103Content == null || mt103Content.isEmpty()) {
            errors.add("MT103 content is null or empty");
            return errors;
        }

        // Check for mandatory header blocks
        if (!mt103Content.contains("{1:")) {
            errors.add("Missing header block 1 {1:");
        }

        if (!mt103Content.contains("{2:")) {
            errors.add("Missing header block 2 {2:");
        }

        if (!mt103Content.contains("{3:")) {
            errors.add("Missing header block 3 {3:");
        }

        return errors;
    }

    public List<String> validateFieldStructure(String mt103Content) {
        List<String> errors = new ArrayList<>();

        // Check for corrupted field separators
        if (mt103Content.contains(";") && mt103Content.contains(":")) {
            // Check if semicolons are used instead of colons in field tags
            String[] lines = mt103Content.split("\n");
            for (String line : lines) {
                if (line.contains(";") && line.matches(".*[0-9]{2}[A-Z]?;.*")) {
                    errors.add("Invalid field separator ';' found instead of ':' in: " + line.trim());
                }
            }
        }

        // Check for double colons
        if (mt103Content.contains("::")) {
            errors.add("Invalid double colon '::' found in message");
        }

        // Check for missing colons in field tags
        String[] lines = mt103Content.split("\n");
        for (String line : lines) {
            if (line.matches(".*[0-9]{2}[A-Z]?/.*") && !line.contains(":")) {
                errors.add("Missing colon in field: " + line.trim());
            }
        }

        return errors;
    }

    public List<String> validateMessageCompleteness(String mt103Content) {
        List<String> errors = new ArrayList<>();

        // Check for trailer block
        if (!mt103Content.contains("{5:")) {
            errors.add("Missing trailer block {5: - message appears to be truncated");
        }

        // Check if message ends abruptly
        if (!mt103Content.trim().endsWith("}")) {
            errors.add("Message appears to be truncated - missing closing brace");
        }

        return errors;
    }

    public List<String> validateCharacters(String mt103Content) {
        List<String> errors = new ArrayList<>();

        // Check for non-printable control characters
        for (int i = 0; i < mt103Content.length(); i++) {
            char c = mt103Content.charAt(i);
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                errors.add("Invalid control character found at position " + i + " (ASCII code: " + (int)c + ")");
            }
        }

        return errors;
    }
}
=====
        package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import com.toufik.trxvalidationservice.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MT103ValidationService {

    @Autowired
    private MT103FieldValidator fieldValidator;

    @Autowired
    private MT103StructureValidator structureValidator;

    @Autowired
    private TransactionFileService fileService;

    public ValidationResult validateTransaction(TransactionWithMT103Event transactionEvent) {
        String transactionId = transactionEvent.getTransaction().getTransactionId();
        String mt103Content = transactionEvent.getMt103Content();

        log.debug("Starting validation for transaction: {}", transactionId);

        List<String> errors = new ArrayList<>();

        // 1. Check for missing mandatory fields
        errors.addAll(fieldValidator.validateMandatoryFields(mt103Content));

        // 2. Validate BIC format
        errors.addAll(fieldValidator.validateBICFormat(mt103Content));

        // 3. Validate date format
        errors.addAll(fieldValidator.validateDateFormat(mt103Content));

        // 4. Validate amount format
        errors.addAll(fieldValidator.validateAmountFormat(mt103Content));

        // 5. Validate structure
        errors.addAll(structureValidator.validateMessageStructure(mt103Content));

        // 6. Validate field structure
        errors.addAll(structureValidator.validateFieldStructure(mt103Content));

        // 7. Check message completeness
        errors.addAll(structureValidator.validateMessageCompleteness(mt103Content));

        // 8. Check for invalid characters
        errors.addAll(structureValidator.validateCharacters(mt103Content));

        boolean isValid = errors.isEmpty();

        // Save to appropriate file
        if (isValid) {
            fileService.saveValidTransaction(transactionEvent);
        } else {
            fileService.saveInvalidTransaction(transactionEvent, errors);
        }

        log.info("Transaction {} validation result: {} (Errors: {})",
                transactionId, isValid ? "VALID" : "INVALID", errors.size());

        return new ValidationResult(isValid, errors);
    }
}
===
        package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import com.toufik.trxvalidationservice.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionConsumerService {

    @Autowired
    private TransactionProducerService transactionProducerService;

    @Autowired
    private MT103ValidationService validationService;

    @KafkaListener(
            topics = "transaction_generator",
            groupId = "transaction-validator-group"
    )
    public void consumeTransaction(
            @Payload TransactionWithMT103Event transactionWithMT103Event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        try {
            log.info("======================= CONSUMED TRANSACTION =============================");
            log.info("Kafka Message Details:");
            log.info("  Topic: {}", topic);
            log.info("Transaction Details:");
            log.info("  Transaction ID: {}", transactionWithMT103Event.getTransaction().getTransactionId());
            log.info("  From Account: {}", transactionWithMT103Event.getTransaction().getFromAccount());
            log.info("  To Account: {}", transactionWithMT103Event.getTransaction().getToAccount());
            log.info("  Amount: {} {}",
                    transactionWithMT103Event.getTransaction().getAmount(),
                    transactionWithMT103Event.getTransaction().getCurrency());
            log.info("  From Bank: {}", transactionWithMT103Event.getTransaction().getFromBankName());
            log.info("  To Bank: {}", transactionWithMT103Event.getTransaction().getToBankName());
            log.info("  Status: {}", transactionWithMT103Event.getTransaction().getStatus());
            log.info("  Timestamp: {}", transactionWithMT103Event.getTransaction().getTimestamp());

            log.info("MT103 Content Preview:");
            String mt103Content = transactionWithMT103Event.getMt103Content();
            if (mt103Content != null) {
                String[] lines = mt103Content.split("\n");
                for (int i = 0; i < Math.min(5, lines.length); i++) {
                    log.info("  {}", lines[i]);
                }
                if (lines.length > 5) {
                    log.info("  ... ({} more lines)", lines.length - 5);
                }
            } else {
                log.warn("  MT103 content is null");
            }

            // ===== VALIDATION LOGIC =====
            log.info("Starting MT103 validation...");
            ValidationResult validationResult = validationService.validateTransaction(transactionWithMT103Event);

            if (validationResult.isValid()) {
                log.info("✓ Transaction {} is VALID - forwarding to alert topic",
                        transactionWithMT103Event.getTransaction().getTransactionId());

                // Only produce valid transactions
                transactionProducerService.sendTransactionAlert(transactionWithMT103Event);

                log.info("Transaction {} processed and forwarded successfully",
                        transactionWithMT103Event.getTransaction().getTransactionId());
            } else {
                log.warn("✗ Transaction {} is INVALID - NOT forwarding. Errors: {}",
                        transactionWithMT103Event.getTransaction().getTransactionId(),
                        validationResult.getErrors());

                log.info("Invalid transaction saved to file for analysis");
            }

            log.info("==================================================================");

        } catch (Exception e) {
            log.error("Error processing transaction {}: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    e.getMessage(), e);
        }
    }
}
===
        package com.toufik.trxvalidationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toufik.trxvalidationservice.model.InvalidTransactionRecord;
import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class TransactionFileService {

    @Value("${transaction.file.valid.path:src/main/resources/data/valid-transactions}")
    private String validTransactionsPath;

    @Value("${transaction.file.invalid.path:src/main/resources/data/invalid-transactions}")
    private String invalidTransactionsPath;

    private final ObjectMapper objectMapper;

    public TransactionFileService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void saveValidTransaction(TransactionWithMT103Event transactionEvent) {
        try {
            // Create directory if not exists
            createDirectoryIfNotExists(validTransactionsPath);

            // Save individual transaction file
            String fileName = generateFileName(transactionEvent.getTransaction().getTransactionId(), "valid");
            String filePath = validTransactionsPath + File.separator + fileName;
            objectMapper.writeValue(new File(filePath), transactionEvent);

            log.info("Valid transaction saved to: {}", filePath);

            // Update the consolidated valid transactions file
            updateValidTransactionsFile(transactionEvent);

        } catch (IOException e) {
            log.error("Error saving valid transaction: {}", e.getMessage(), e);
        }
    }

    public void saveInvalidTransaction(TransactionWithMT103Event transactionEvent, List<String> errors) {
        try {
            // Create directory if not exists
            createDirectoryIfNotExists(invalidTransactionsPath);

            // Create invalid transaction record
            InvalidTransactionRecord record = new InvalidTransactionRecord(transactionEvent, errors);

            // Save individual transaction file
            String fileName = generateFileName(transactionEvent.getTransaction().getTransactionId(), "invalid");
            String filePath = invalidTransactionsPath + File.separator + fileName;
            objectMapper.writeValue(new File(filePath), record);

            log.info("Invalid transaction saved to: {} with {} errors", filePath, errors.size());

            // Update the consolidated invalid transactions file
            updateInvalidTransactionsFile(record);

        } catch (IOException e) {
            log.error("Error saving invalid transaction: {}", e.getMessage(), e);
        }
    }

    private void updateValidTransactionsFile(TransactionWithMT103Event transactionEvent) {
        try {
            String consolidatedFilePath = "src/main/resources/valid-transactions.json";
            List<TransactionWithMT103Event> validTransactions = readValidTransactions();

            // Add new transaction
            validTransactions.add(transactionEvent);

            // Ensure parent directory exists
            File file = new File(consolidatedFilePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Write back to file
            objectMapper.writeValue(file, validTransactions);
            log.info("Updated consolidated valid transactions file with {} total transactions", validTransactions.size());

        } catch (IOException e) {
            log.error("Error updating consolidated valid transactions file: {}", e.getMessage(), e);
        }
    }

    private void updateInvalidTransactionsFile(InvalidTransactionRecord record) {
        try {
            String consolidatedFilePath = "src/main/resources/invalid-transactions.json";
            List<InvalidTransactionRecord> invalidTransactions = readInvalidTransactions();

            // Add new transaction
            invalidTransactions.add(record);

            // Ensure parent directory exists
            File file = new File(consolidatedFilePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Write back to file
            objectMapper.writeValue(file, invalidTransactions);
            log.info("Updated consolidated invalid transactions file with {} total transactions", invalidTransactions.size());

        } catch (IOException e) {
            log.error("Error updating consolidated invalid transactions file: {}", e.getMessage(), e);
        }
    }

    public List<TransactionWithMT103Event> readValidTransactions() {
        try {
            String consolidatedFilePath = "src/main/resources/valid-transactions.json";
            File file = new File(consolidatedFilePath);

            if (!file.exists()) {
                // Create empty file if it doesn't exist
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                List<TransactionWithMT103Event> emptyList = new ArrayList<>();
                objectMapper.writeValue(file, emptyList);
                return emptyList;
            }

            TransactionWithMT103Event[] transactions = objectMapper.readValue(file, TransactionWithMT103Event[].class);
            return new ArrayList<>(Arrays.asList(transactions));

        } catch (IOException e) {
            log.error("Error reading valid transactions: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<InvalidTransactionRecord> readInvalidTransactions() {
        try {
            String consolidatedFilePath = "src/main/resources/invalid-transactions.json";
            File file = new File(consolidatedFilePath);

            if (!file.exists()) {
                // Create empty file if it doesn't exist
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                List<InvalidTransactionRecord> emptyList = new ArrayList<>();
                objectMapper.writeValue(file, emptyList);
                return emptyList;
            }

            InvalidTransactionRecord[] transactions = objectMapper.readValue(file, InvalidTransactionRecord[].class);
            return new ArrayList<>(Arrays.asList(transactions));

        } catch (IOException e) {
            log.error("Error reading invalid transactions: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void createDirectoryIfNotExists(String path) {
        try {
            Path directoryPath = Paths.get(path);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                log.info("Created directory: {}", path);
            }
        } catch (IOException e) {
            log.error("Could not create directory {}: {}", path, e.getMessage());
        }
    }

    private String generateFileName(String transactionId, String type) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s_%s.json", type, transactionId, timestamp);
    }
}
====
        package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TransactionProducerService {

    private static final String TOPIC = "transactions_alert";

    @Autowired
    private KafkaTemplate<String, TransactionWithMT103Event> kafkaTemplate;

    public void sendTransactionAlert(TransactionWithMT103Event transactionWithMT103Event) {
        try {

            log.info("======================= PRODUCING TRANSACTION =============================");
            log.info("Kafka Message Details:");
            log.info("Transaction Details:");
            log.info("  Transaction ID: {}", transactionWithMT103Event.getTransaction().getTransactionId());
            log.info("  From Account: {}", transactionWithMT103Event.getTransaction().getFromAccount());
            log.info("  To Account: {}", transactionWithMT103Event.getTransaction().getToAccount());
            log.info("  Amount: {} {}",
                    transactionWithMT103Event.getTransaction().getAmount(),
                    transactionWithMT103Event.getTransaction().getCurrency());
            log.info("  From Bank: {}", transactionWithMT103Event.getTransaction().getFromBankName());
            log.info("  To Bank: {}", transactionWithMT103Event.getTransaction().getToBankName());
            log.info("  Status: {}", transactionWithMT103Event.getTransaction().getStatus());
            log.info("  Timestamp: {}", transactionWithMT103Event.getTransaction().getTimestamp());

            log.info("MT103 Content Preview:");

            String key = transactionWithMT103Event.getTransaction().getTransactionId();

            log.info("Sending transaction alert to topic '{}' with key '{}'", TOPIC, key);

            CompletableFuture<SendResult<String, TransactionWithMT103Event>> future =
                    kafkaTemplate.send(TOPIC, key, transactionWithMT103Event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent transaction alert for transaction '{}' with offset=[{}]",
                            key, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send transaction alert for transaction '{}': {}",
                            key, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error sending transaction alert for transaction '{}': {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    e.getMessage(), e);
            throw new RuntimeException("Failed to send transaction alert", e);
        }
    }
}
=======
        package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TransactionValidationService {

    private final LenientMT103FieldValidator fieldValidator;
    private final TransactionFileService fileService;
    private final DebugTransactionService debugService;

    @Autowired
    public TransactionValidationService(LenientMT103FieldValidator fieldValidator,
                                        TransactionFileService fileService,
                                        DebugTransactionService debugService) {
        this.fieldValidator = fieldValidator;
        this.fileService = fileService;
        this.debugService = debugService;
    }

    @KafkaListener(topics = "transaction-events", groupId = "transaction-validator-group")
    public void processTransaction(TransactionWithMT103Event transactionEvent) {
        try {
            log.info("Processing transaction: {}", transactionEvent.getTransaction().getTransactionId());

            // Debug the transaction first
            debugService.debugTransaction(transactionEvent);

            // Validate the transaction
            List<String> validationErrors = validateTransaction(transactionEvent);

            if (validationErrors.isEmpty()) {
                // Transaction is valid
                log.info("Transaction {} is VALID", transactionEvent.getTransaction().getTransactionId());
                fileService.saveValidTransaction(transactionEvent);
            } else {
                // Transaction is invalid
                log.warn("Transaction {} is INVALID with {} errors: {}",
                        transactionEvent.getTransaction().getTransactionId(),
                        validationErrors.size(),
                        validationErrors);
                fileService.saveInvalidTransaction(transactionEvent, validationErrors);
            }

        } catch (Exception e) {
            log.error("Error processing transaction {}: {}",
                    transactionEvent.getTransaction().getTransactionId(), e.getMessage(), e);
        }
    }

    private List<String> validateTransaction(TransactionWithMT103Event transactionEvent) {
        List<String> allErrors = new ArrayList<>();

        String mt103Content = transactionEvent.getMt103Content();

        // Validate mandatory fields
        allErrors.addAll(fieldValidator.validateMandatoryFields(mt103Content));

        // Validate BIC format
        allErrors.addAll(fieldValidator.validateBICFormat(mt103Content));

        // Validate date format
        allErrors.addAll(fieldValidator.validateDateFormat(mt103Content));

        // Validate amount format
        allErrors.addAll(fieldValidator.validateAmountFormat(mt103Content));

        log.debug("Validation completed for transaction {} with {} total errors",
                transactionEvent.getTransaction().getTransactionId(), allErrors.size());

        return allErrors;
    }
}
=====