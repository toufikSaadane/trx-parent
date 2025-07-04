package com.toufik.trxalertservice.controller;

import com.toufik.trxalertservice.entity.TransactionEntity;
import com.toufik.trxalertservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionEntity>> getAllTransactions() {
        log.info("Fetching all transactions");
        List<TransactionEntity> transactions = transactionService.getAllTransactions();
        log.info("Found {} transactions", transactions.size());
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/fraudulent")
    public ResponseEntity<List<TransactionEntity>> getFraudulentTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Fetching fraudulent transactions - Start: {}, End: {}", start, end);

        List<TransactionEntity> fraudulentTransactions;
        if (start != null && end != null) {
            fraudulentTransactions = transactionService.getFraudulentTransactionsBetween(start, end);
        } else {
            fraudulentTransactions = transactionService.getFraudulentTransactions();
        }

        return ResponseEntity.ok(fraudulentTransactions);
    }
}