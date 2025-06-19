package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.FraudDetectionRule;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

@Service
@Slf4j
public class StructuringDetectionService implements FraudDetectionRule {

    private static final BigDecimal STRUCTURING_THRESHOLD = new BigDecimal("1000.00");
    private static final int MAX_SMALL_TRANSACTIONS = 5;
    private static final int TIME_WINDOW_MINUTES = 60;

    // In-memory storage for demo - in production, use Redis or database
    private final ConcurrentMap<String, TransactionHistory> accountTransactions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    public StructuringDetectionService() {
        // Schedule cleanup every 30 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldRecords, 30, 30, TimeUnit.MINUTES);
    }

    @Override
    public boolean isSuspicious(TransactionWithMT103Event event) {
        Transaction transaction = event.getTransaction();
        String fromAccount = transaction.getFromAccount();
        BigDecimal amount = transaction.getAmount();
        LocalDateTime timestamp = transaction.getTimestamp();

        log.debug("Checking structuring pattern for transaction {} from account {} with amount {}",
                transaction.getTransactionId(), fromAccount, amount);

        // Check if amount is below threshold
        if (amount.compareTo(STRUCTURING_THRESHOLD) >= 0) {
            log.debug("Transaction amount {} exceeds structuring threshold {}", amount, STRUCTURING_THRESHOLD);
            return false;
        }

        // Get or create transaction history for this account
        TransactionHistory history = accountTransactions.computeIfAbsent(fromAccount,
                k -> new TransactionHistory());

        // Add current transaction
        history.addTransaction(timestamp, amount);

        // Count transactions within time window
        int recentTransactionCount = history.getTransactionCountInWindow(timestamp, TIME_WINDOW_MINUTES);
        BigDecimal totalAmount = history.getTotalAmountInWindow(timestamp, TIME_WINDOW_MINUTES);

        log.debug("Account {} has {} transactions totaling {} in last {} minutes",
                fromAccount, recentTransactionCount, totalAmount, TIME_WINDOW_MINUTES);

        // Check if suspicious pattern detected
        boolean isSuspicious = recentTransactionCount >= MAX_SMALL_TRANSACTIONS;

        if (isSuspicious) {
            log.warn("STRUCTURING DETECTED: Account {} has {} small transactions totaling {} in last {} minutes",
                    fromAccount, recentTransactionCount, totalAmount, TIME_WINDOW_MINUTES);
        }

        return isSuspicious;
    }

    @Override
    public String getRuleName() {
        return "STRUCTURING_DETECTION";
    }

    @Override
    public String getDescription() {
        return "Detects multiple small transactions under €1000 within short time period";
    }

    private void cleanupOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2); // Keep 2 hours of data
        log.debug("Cleaning up transaction records older than {}", cutoff);

        accountTransactions.values().forEach(history -> history.removeOldTransactions(cutoff));

        // Remove empty histories
        accountTransactions.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        log.debug("Cleanup completed. Active accounts being monitored: {}", accountTransactions.size());
    }

    private static class TransactionHistory {
        private final List<TransactionRecord> transactions = new ArrayList<>();

        public synchronized void addTransaction(LocalDateTime timestamp, BigDecimal amount) {
            transactions.add(new TransactionRecord(timestamp, amount));
            log.debug("Added transaction. Total transactions in history: {}", transactions.size());
        }

        public synchronized int getTransactionCountInWindow(LocalDateTime currentTime, int windowMinutes) {
            LocalDateTime windowStart = currentTime.minusMinutes(windowMinutes);

            int count = (int) transactions.stream()
                    .filter(tx -> tx.timestamp.isAfter(windowStart) || tx.timestamp.isEqual(windowStart))
                    .count();

            log.debug("Found {} transactions within {} minute window from {}", count, windowMinutes, windowStart);
            return count;
        }

        public synchronized BigDecimal getTotalAmountInWindow(LocalDateTime currentTime, int windowMinutes) {
            LocalDateTime windowStart = currentTime.minusMinutes(windowMinutes);

            return transactions.stream()
                    .filter(tx -> tx.timestamp.isAfter(windowStart) || tx.timestamp.isEqual(windowStart))
                    .map(tx -> tx.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public synchronized void removeOldTransactions(LocalDateTime cutoff) {
            transactions.removeIf(tx -> tx.timestamp.isBefore(cutoff));
        }

        public synchronized boolean isEmpty() {
            return transactions.isEmpty();
        }
    }

    private static class TransactionRecord {
        final LocalDateTime timestamp;
        final BigDecimal amount;

        TransactionRecord(LocalDateTime timestamp, BigDecimal amount) {
            this.timestamp = timestamp;
            this.amount = amount;
        }
    }
}