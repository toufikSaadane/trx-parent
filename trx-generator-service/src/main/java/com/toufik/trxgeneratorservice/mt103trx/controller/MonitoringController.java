package com.toufik.trxgeneratorservice.mt103trx.controller;

import com.toufik.trxgeneratorservice.mt103trx.entity.TransactionEntity;
import com.toufik.trxgeneratorservice.mt103trx.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    private final TransactionRepository transactionRepository;

    public MonitoringController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Get basic stats
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = transactionRepository.count();
        long normal = transactionRepository.countByTransactionType("NORMAL");
        long fraud = transactionRepository.countByTransactionType("FRAUD");
        long invalid = transactionRepository.countByTransactionType("INVALID");

        stats.put("total", total);
        stats.put("normal", normal);
        stats.put("fraud", fraud);
        stats.put("invalid", invalid);

        return stats;
    }

    // Get recent 10 transactions
    @GetMapping("/recent")
    public List<TransactionEntity> getRecentTransactions() {
        return transactionRepository.findRecentTransactions()
                .stream()
                .limit(10)
                .toList();
    }

    // Get all fraud transactions
    @GetMapping("/fraud")
    public List<TransactionEntity> getFraudTransactions() {
        return transactionRepository.findByTransactionType("FRAUD");
    }

    // Get transactions by type
    @GetMapping("/type/{type}")
    public List<TransactionEntity> getTransactionsByType(@PathVariable String type) {
        return transactionRepository.findByTransactionType(type.toUpperCase());
    }

    // Get transactions from today
    @GetMapping("/today")
    public List<TransactionEntity> getTodayTransactions() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        return transactionRepository.findByTimestampBetween(startOfDay, endOfDay);
    }

    // Get all transactions (for table view)
    @GetMapping("/all")
    public List<TransactionEntity> getAllTransactions() {
        return transactionRepository.findAll();
    }
}