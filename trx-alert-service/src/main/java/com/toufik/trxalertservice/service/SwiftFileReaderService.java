package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@Slf4j
public class SwiftFileReaderService {

    @Value("${swift.output.directory:src/main/resources/swift-files}")
    private String outputDirectory;

    @Value("${swift.output.file-extension:.swift}")
    private String fileExtension;

    private static List<String> TRX_STATUS = List.of(
            "RECEIVED",
            "VALIDATED",
            "FRAUD_SUSPECTED",
            "FRAUD_CONFIRMED",
            "FRAUD_CLEARED",
            "PROCESSED",
            "REJECTED",
            "ERROR",
            "RETRYING",
            "QUARANTINED",
            "FORWARDED"
    );

    public List<TransactionWithMT103Event> readAllSwiftFiles() {
        List<TransactionWithMT103Event> transactions = new ArrayList<>();

        try {
            Path dirPath = Paths.get(outputDirectory);

            if (!Files.exists(dirPath)) {
                log.warn("SWIFT files directory does not exist: {}", outputDirectory);
                return transactions;
            }

            try (Stream<Path> paths = Files.walk(dirPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(fileExtension))
                        .forEach(path -> {
                            try {
                                TransactionWithMT103Event transaction = parseSwiftFile(path);
                                if (transaction != null) {
                                    transactions.add(transaction);
                                }
                            } catch (Exception e) {
                                log.error("Error parsing SWIFT file {}: {}", path.getFileName(), e.getMessage());
                            }
                        });
            }

            log.info("Successfully read {} SWIFT files from {}", transactions.size(), outputDirectory);

        } catch (IOException e) {
            log.error("Error reading SWIFT files from directory {}: {}", outputDirectory, e.getMessage());
            throw new RuntimeException("Failed to read SWIFT files", e);
        }

        return transactions;
    }

    private TransactionWithMT103Event parseSwiftFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        String[] lines = content.split("\n");

        Transaction transaction = new Transaction();
        StringBuilder mt103Content = new StringBuilder();
        boolean inMT103Section = false;

        for (String line : lines) {
            line = line.trim();

            // Skip empty lines
            if (line.isEmpty()) {
                continue;
            }

            // Parse header comments for transaction metadata
            if (line.startsWith("// Transaction ID:")) {
                transaction.setTransactionId(extractValue(line, "// Transaction ID:"));
            } else if (line.startsWith("// Amount:")) {
                parseAmount(line, transaction);
            } else if (line.startsWith("// From:")) {
                transaction.setFromBankName(extractValue(line, "// From:"));
            } else if (line.startsWith("// To:")) {
                transaction.setToBankName(extractValue(line, "// To:"));
            } else if (line.startsWith("// Generated at:")) {
                parseTimestamp(line, transaction);
            } else if (line.equals("//=====================================")) {
                inMT103Section = true;
                continue;
            }

            // Collect MT103 content (everything after the separator that's not a comment)
            if (inMT103Section && !line.startsWith("//")) {
                if (mt103Content.length() > 0) {
                    mt103Content.append("\n");
                }
                mt103Content.append(line);
            }
        }

        // Extract additional information from MT103 content if available
        String mt103String = mt103Content.toString();
        if (!mt103String.isEmpty() && !mt103String.contains("No MT103 content available")) {
            parseAdditionalFieldsFromMT103(mt103String, transaction);
        }

        // Set default values if not found
        setDefaultValues(transaction, filePath.getFileName().toString());

        TransactionWithMT103Event event = new TransactionWithMT103Event();
        event.setTransaction(transaction);
        event.setMt103Content(mt103String.isEmpty() ? null : mt103String);

        return event;
    }

    private String extractValue(String line, String prefix) {
        return line.substring(prefix.length()).trim();
    }

    private void parseAmount(String line, Transaction transaction) {
        String amountStr = extractValue(line, "// Amount:");
        String[] parts = amountStr.split(" ");
        if (parts.length >= 2) {
            try {
                transaction.setAmount(new BigDecimal(parts[0]));
                transaction.setCurrency(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse amount: {}", amountStr);
            }
        }
    }

    private void parseTimestamp(String line, Transaction transaction) {
        String timestampStr = extractValue(line, "// Generated at:");
        try {
            // Try to parse the timestamp (adjust format as needed)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            transaction.setTimestamp(LocalDateTime.parse(timestampStr, formatter));
        } catch (DateTimeParseException e) {
            try {
                // Try alternative format
                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                transaction.setTimestamp(LocalDateTime.parse(timestampStr, altFormatter));
            } catch (DateTimeParseException e2) {
                log.warn("Failed to parse timestamp: {}", timestampStr);
                transaction.setTimestamp(LocalDateTime.now());
            }
        }
    }

    private void parseAdditionalFieldsFromMT103(String mt103Content, Transaction transaction) {
        // Parse MT103 fields to extract additional transaction information
        // This is a simplified parser - you might need to adjust based on your MT103 format

        // Example MT103 field patterns
        parseFieldFromMT103(mt103Content, ":20:", transaction::setTransactionId);
        parseFieldFromMT103(mt103Content, ":32A:", (value) -> {
            // Format: YYMMDDCCCAMOUNT
            if (value.length() > 9) {
                String currency = value.substring(6, 9);
                String amount = value.substring(9);
                transaction.setCurrency(currency);
                try {
                    // Remove any non-numeric characters except decimal point
                    String cleanAmount = amount.replaceAll("[^0-9.]", "");
                    transaction.setAmount(new BigDecimal(cleanAmount));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse amount from MT103: {}", amount);
                }
            }
        });

        parseFieldFromMT103(mt103Content, ":50K:", (value) -> {
            // Ordering customer - extract account info
            String[] lines = value.split("\n");
            if (lines.length > 0) {
                transaction.setFromAccount(lines[0].trim());
            }
        });

        parseFieldFromMT103(mt103Content, ":59:", (value) -> {
            // Beneficiary customer - extract account info
            String[] lines = value.split("\n");
            if (lines.length > 0) {
                transaction.setToAccount(lines[0].trim());
            }
        });

        parseFieldFromMT103(mt103Content, ":52A:", (value) -> {
            // Ordering institution
            if (value.length() > 8) {
                transaction.setFromBankSwift(value.substring(0, 8));
            }
        });

        parseFieldFromMT103(mt103Content, ":57A:", (value) -> {
            // Account with institution
            if (value.length() > 8) {
                transaction.setToBankSwift(value.substring(0, 8));
            }
        });
    }

    private void parseFieldFromMT103(String mt103Content, String fieldCode, java.util.function.Consumer<String> setter) {
        Pattern pattern = Pattern.compile(fieldCode + "([^\n:]+(?:\n[^:][^\n]*)*)");
        Matcher matcher = pattern.matcher(mt103Content);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            setter.accept(value);
        }
    }

    private void setDefaultValues(Transaction transaction, String filename) {
        if (transaction.getTransactionId() == null) {
            // Extract transaction ID from filename if not found in content
            Pattern pattern = Pattern.compile("MT103_([^_]+)_");
            Matcher matcher = pattern.matcher(filename);
            if (matcher.find()) {
                transaction.setTransactionId(matcher.group(1));
            } else {
                transaction.setTransactionId("UNKNOWN_" + System.currentTimeMillis());
            }
        }

        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }

        if (transaction.getStatus() == null) {
            transaction.setStatus(TRX_STATUS.get(new Random().nextInt(TRX_STATUS.size())));
        }

        if (transaction.getAmount() == null) {
            transaction.setAmount(BigDecimal.ZERO);
        }

        if (transaction.getCurrency() == null) {
            transaction.setCurrency("USD");
        }
    }
}