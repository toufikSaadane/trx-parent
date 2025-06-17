package com.toufik.trxalertservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
public class TransactionReportController {

    @Value("${fraud.output.directory:src/main/resources/fraud-detection}")
    private String outputDirectory;

    @Value("${fraud.output.normal-transactions-file:normal_transactions.json}")
    private String normalTransactionsFile;

    @Value("${fraud.output.fraudulent-transactions-file:fraudulent_transactions.json}")
    private String fraudulentTransactionsFile;

    private final ObjectMapper objectMapper;

    public TransactionReportController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * GET endpoint to retrieve normal transactions
     * @return ResponseEntity containing normal transactions JSON data
     */
    @GetMapping(value = "/normal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNormalTransactions() {
        log.info("Request received for normal transactions");

        try {
            Path filePath = Paths.get(outputDirectory, normalTransactionsFile);

            if (!Files.exists(filePath)) {
                log.warn("Normal transactions file not found: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Normal transactions file not found",
                                "message", "No normal transactions data available yet"
                        ));
            }

            String jsonContent = Files.readString(filePath);
            Map<String, Object> transactionData = objectMapper.readValue(jsonContent, Map.class);

            log.info("Successfully retrieved normal transactions - Total count: {}",
                    transactionData.get("total_count"));

            return ResponseEntity.ok(transactionData);

        } catch (IOException e) {
            log.error("Error reading normal transactions file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to read normal transactions",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * GET endpoint to retrieve fraudulent transactions
     * @return ResponseEntity containing fraudulent transactions JSON data
     */
    @GetMapping(value = "/fraudulent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFraudulentTransactions() {
        log.info("Request received for fraudulent transactions");

        try {
            Path filePath = Paths.get(outputDirectory, fraudulentTransactionsFile);

            if (!Files.exists(filePath)) {
                log.warn("Fraudulent transactions file not found: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Fraudulent transactions file not found",
                                "message", "No fraudulent transactions data available yet"
                        ));
            }

            String jsonContent = Files.readString(filePath);
            Map<String, Object> transactionData = objectMapper.readValue(jsonContent, Map.class);

            log.info("Successfully retrieved fraudulent transactions - Total count: {}",
                    transactionData.get("total_count"));

            return ResponseEntity.ok(transactionData);

        } catch (IOException e) {
            log.error("Error reading fraudulent transactions file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to read fraudulent transactions",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * GET endpoint to retrieve transaction statistics
     * @return ResponseEntity containing transaction statistics
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTransactionStatistics() {
        log.info("Request received for transaction statistics");

        try {
            Path normalPath = Paths.get(outputDirectory, normalTransactionsFile);
            Path fraudulentPath = Paths.get(outputDirectory, fraudulentTransactionsFile);

            int normalCount = 0;
            int fraudulentCount = 0;

            // Get normal transaction count
            if (Files.exists(normalPath)) {
                String normalContent = Files.readString(normalPath);
                Map<String, Object> normalData = objectMapper.readValue(normalContent, Map.class);
                normalCount = (Integer) normalData.getOrDefault("total_count", 0);
            }

            // Get fraudulent transaction count
            if (Files.exists(fraudulentPath)) {
                String fraudulentContent = Files.readString(fraudulentPath);
                Map<String, Object> fraudulentData = objectMapper.readValue(fraudulentContent, Map.class);
                fraudulentCount = (Integer) fraudulentData.getOrDefault("total_count", 0);
            }

            int totalCount = normalCount + fraudulentCount;
            double fraudulentPercentage = totalCount > 0 ? (double) fraudulentCount / totalCount * 100.0 : 0.0;

            Map<String, Object> statistics = Map.of(
                    "normal_transaction_count", normalCount,
                    "fraudulent_transaction_count", fraudulentCount,
                    "total_transaction_count", totalCount,
                    "fraudulent_percentage", Math.round(fraudulentPercentage * 100.0) / 100.0
            );

            log.info("Transaction statistics - Normal: {}, Fraudulent: {}, Total: {}, Fraud %: {}%",
                    normalCount, fraudulentCount, totalCount, fraudulentPercentage);

            return ResponseEntity.ok(statistics);

        } catch (IOException e) {
            log.error("Error reading transaction statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to read transaction statistics",
                            "message", e.getMessage()
                    ));
        }
    }
}