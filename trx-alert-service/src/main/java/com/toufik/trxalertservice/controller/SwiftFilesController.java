package com.toufik.trxalertservice.controller;

import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import com.toufik.trxalertservice.service.SwiftFileReaderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/swift")
@Slf4j
public class SwiftFilesController {

    @Autowired
    private SwiftFileReaderService swiftFileReaderService;

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionWithMT103Event>> getAllTransactions() {
        try {
            log.info("Received request to get all SWIFT transactions");

            List<TransactionWithMT103Event> transactions = swiftFileReaderService.readAllSwiftFiles();

            log.info("Successfully retrieved {} transactions from SWIFT files", transactions.size());

            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            log.error("Error retrieving SWIFT transactions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/transactions/count")
    public ResponseEntity<Map<String, Object>> getTransactionCount() {
        try {
            log.info("Received request to get SWIFT transaction count");

            List<TransactionWithMT103Event> transactions = swiftFileReaderService.readAllSwiftFiles();

            Map<String, Object> response = Map.of(
                    "count", transactions.size(),
                    "timestamp", java.time.LocalDateTime.now()
            );

            log.info("Transaction count: {}", transactions.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting transaction count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionWithMT103Event> getTransactionById(@PathVariable String transactionId) {
        try {
            log.info("Received request to get transaction by ID: {}", transactionId);

            List<TransactionWithMT103Event> transactions = swiftFileReaderService.readAllSwiftFiles();

            TransactionWithMT103Event foundTransaction = transactions.stream()
                    .filter(t -> transactionId.equals(t.getTransaction().getTransactionId()))
                    .findFirst()
                    .orElse(null);

            if (foundTransaction != null) {
                log.info("Found transaction with ID: {}", transactionId);
                return ResponseEntity.ok(foundTransaction);
            } else {
                log.warn("Transaction not found with ID: {}", transactionId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error retrieving transaction {}: {}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = Map.of(
                "status", "UP",
                "service", "SWIFT Files API",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(response);
    }
}