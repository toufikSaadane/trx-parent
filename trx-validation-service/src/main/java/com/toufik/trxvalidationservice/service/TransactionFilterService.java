package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.Transaction;
import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import com.toufik.trxvalidationservice.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TransactionFilterService {

    private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+([,.]\\d{1,2})?$");

    @Autowired
    private TransactionProducerService producerService;

    @Autowired
    private TransactionRepository transactionRepository;

    public void process(TransactionWithMT103Event event) {
        String transactionId = event.getTransaction().getTransactionId();

        try {
            String validationResult = validateMT103(event.getMt103Content());

            if (validationResult == null) {
                producerService.sendTransactionAlert(event);
                log.info("Valid transaction forwarded: {}", transactionId);

                // Save valid transaction
                saveTransaction(event.getTransaction(), true, "Valid");
            } else {
                log.warn("Transaction filtered: {} - Reason: {}", transactionId, validationResult);

                // Save invalid transaction
                saveTransaction(event.getTransaction(), false, validationResult);
            }

        } catch (Exception e) {
            log.error("Error processing transaction {}: {}", transactionId, e.getMessage());
        }
    }

    private void saveTransaction(Transaction originalTransaction, boolean isValid, String validationReason) {
        try {
            Transaction transactionToSave = Transaction.builder()
                    .transactionId(originalTransaction.getTransactionId())
                    .fromAccount(originalTransaction.getFromAccount())
                    .toAccount(originalTransaction.getToAccount())
                    .amount(originalTransaction.getAmount())
                    .currency(originalTransaction.getCurrency())
                    .fromBankSwift(originalTransaction.getFromBankSwift())
                    .toBankSwift(originalTransaction.getToBankSwift())
                    .fromBankName(originalTransaction.getFromBankName())
                    .toBankName(originalTransaction.getToBankName())
                    .timestamp(originalTransaction.getTimestamp())
                    .status(originalTransaction.getStatus())
                    .fromIBAN(originalTransaction.getFromIBAN())
                    .toIBAN(originalTransaction.getToIBAN())
                    .fromCountryCode(originalTransaction.getFromCountryCode())
                    .toCountryCode(originalTransaction.getToCountryCode())
                    .isValid(isValid)
                    .validationReason(validationReason)
                    .processedAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(transactionToSave);
            log.info("Transaction {} saved to database", transactionToSave.getTransactionId());
        } catch (Exception e) {
            log.error("Error saving transaction {}: {}", originalTransaction.getTransactionId(), e.getMessage());
        }
    }

    private String validateMT103(String content) {
        if (content == null || content.isBlank()) {
            return "Content is null or blank";
        }

        // Remove all whitespace and newlines for structure validation
        String cleanContent = content.replaceAll("\\s", "");

        // 1. Check basic structure blocks
        if (!cleanContent.startsWith("{1:") || !cleanContent.contains("{2:") ||
                !cleanContent.contains("{3:") || !cleanContent.contains("{4:") ||
                !cleanContent.contains("{5:") || !cleanContent.endsWith("}")) {
            return "Missing required MT103 structure blocks";
        }

        // 2. Validate header block 1 format
        if (!cleanContent.matches(".*\\{1:F01[A-Z]{11}\\d\\}.*")) {
            return "Invalid header block 1 format";
        }

        // 3. Validate header block 2 format
        if (!cleanContent.matches(".*\\{2:I103[A-Z]{11}\\d[A-Z]\\}.*")) {
            return "Invalid header block 2 format";
        }

        // 4. Check mandatory fields in block 4
        String[] mandatoryFields = {":20:", ":23B:", ":32A:"};
        for (String field : mandatoryFields) {
            if (!content.contains(field)) {
                return "Missing mandatory field: " + field;
            }
        }

        // 5. Validate block 5 trailer format
        if (!cleanContent.matches(".*\\{5:\\{MAC:[A-F0-9]+\\}\\{CHK:[A-F0-9]+\\}\\}$")) {
            return "Invalid trailer block 5 format";
        }

        // 6. Check field structure integrity
        String structureCheck = checkFieldStructure(content);
        if (structureCheck != null) return structureCheck;

        // 7. Validate BIC codes
        String bicCheck = validateBICCodes(content);
        if (bicCheck != null) return bicCheck;

        // 8. Validate date format in :32A:
        String dateCheck = validateDateFormat(content);
        if (dateCheck != null) return dateCheck;

        // 9. Validate amount format in :32A:
        String amountCheck = validateAmountFormat(content);
        if (amountCheck != null) return amountCheck;

        return null;
    }

    private String checkFieldStructure(String content) {
        if (content.contains("::")) {
            return "Invalid field structure: double colons found";
        }

        String[] lines = content.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^:[0-9]{2}[A-Z]?;.*")) {
                return "Invalid field structure: semicolon instead of colon";
            }
        }
        return null;
    }

    private String validateBICCodes(String content) {
        String[] bicFields = {":52A:", ":57A:"};

        for (String field : bicFields) {
            int index = content.indexOf(field);
            if (index != -1) {
                try {
                    String fieldContent = content.substring(index + field.length());
                    int nextFieldIndex = fieldContent.indexOf("\n:");
                    if (nextFieldIndex > 0) {
                        fieldContent = fieldContent.substring(0, nextFieldIndex);
                    }

                    String[] lines = fieldContent.trim().split("\\n");
                    String potentialBIC = lines[0].trim();

                    if (potentialBIC.length() >= 8) {
                        String bicValidation = validateSingleBIC(potentialBIC);
                        if (bicValidation != null) {
                            return "Invalid BIC in field " + field + ": " + bicValidation;
                        }
                    }
                } catch (Exception e) {
                    return "Error parsing BIC in field " + field;
                }
            }
        }
        return null;
    }

    private String validateSingleBIC(String bic) {
        if (bic.length() < 8 || bic.length() > 11) {
            return "BIC length invalid: " + bic;
        }
        if (!BIC_PATTERN.matcher(bic).matches()) {
            return "BIC format invalid: " + bic;
        }
        return null;
    }

    private String validateDateFormat(String content) {
        int index = content.indexOf(":32A:");
        if (index == -1) {
            return null;
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
                return "Invalid date format in :32A:";
            }

            int month = Integer.parseInt(dateStr.substring(2, 4));
            int day = Integer.parseInt(dateStr.substring(4, 6));

            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return "Invalid date values in :32A:";
            }

        } catch (Exception e) {
            return "Error parsing date in :32A:";
        }

        return null;
    }

    private String validateAmountFormat(String content) {
        int index = content.indexOf(":32A:");
        if (index == -1) {
            return null;
        }

        try {
            String fieldContent = content.substring(index + 5).trim();
            int nextFieldIndex = fieldContent.indexOf("\n:");
            if (nextFieldIndex > 0) {
                fieldContent = fieldContent.substring(0, nextFieldIndex);
            }

            if (fieldContent.length() <= 9) {
                return "Amount field too short in :32A:";
            }

            String amountPart = fieldContent.substring(9).trim();
            if (amountPart.isEmpty()) {
                return "Empty amount in :32A:";
            }

            String cleanAmount = amountPart.replaceAll("[^0-9,.-]", "");
            if (!AMOUNT_PATTERN.matcher(cleanAmount).matches()) {
                return "Invalid amount format: " + amountPart;
            }

        } catch (Exception e) {
            return "Error parsing amount in :32A:";
        }

        return null;
    }
}