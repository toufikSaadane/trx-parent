package com.toufik.trxvalidationservice.controller;

import com.toufik.trxvalidationservice.model.Transaction;
import com.toufik.trxvalidationservice.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
public class TransactionValidationController {

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        try {
            List<Transaction> transactions = transactionRepository.findAll();
            log.info("Retrieved {} total transactions", transactions.size());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error retrieving all transactions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/valid")
    public ResponseEntity<List<Transaction>> getValidTransactions() {
        try {
            List<Transaction> validTransactions = transactionRepository.findByIsValid(true);
            log.info("Retrieved {} valid transactions", validTransactions.size());
            return ResponseEntity.ok(validTransactions);
        } catch (Exception e) {
            log.error("Error retrieving valid transactions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/invalid")
    public ResponseEntity<List<Transaction>> getInvalidTransactions() {
        try {
            List<Transaction> invalidTransactions = transactionRepository.findByIsValid(false);
            log.info("Retrieved {} invalid transactions", invalidTransactions.size());
            return ResponseEntity.ok(invalidTransactions);
        } catch (Exception e) {
            log.error("Error retrieving invalid transactions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}