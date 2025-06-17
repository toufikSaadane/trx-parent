package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.FraudScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
public class FraudTransactionGeneratorService {

    @Autowired
    private FraudTransactionFactory fraudTransactionFactory;

    @Autowired
    private TransactionProducer transactionProducer;

    @Autowired
    private FraudMT103MessageFormatter fraudMT103MessageFormatter;

    private final Random random = new Random();
    private final ConcurrentLinkedQueue<Transaction> transactionHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_HISTORY_SIZE = 1000;

    // Weighted fraud scenario selection
    private static final FraudScenario[] FRAUD_SCENARIOS = FraudScenario.values();
    private static final int[] SCENARIO_WEIGHTS = {
            15, // HIGH_AMOUNT_THRESHOLD - 15%
            10, // OFF_HOURS_TRANSACTION - 10%
            20, // SUSPICIOUS_REMITTANCE - 20%
            15, // ROUND_AMOUNT_PATTERN - 15%
            10, // FREQUENT_SMALL_AMOUNTS - 10%
            15, // CROSS_BORDER_HIGH_RISK - 15%
            10, // STRUCTURING_PATTERN - 10%
            5   // CRYPTOCURRENCY_KEYWORDS - 5%
    };

    /**
     * Scheduled method to generate fraud transactions every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void generateAndSendFraudTransaction() {
        try {
            FraudScenario selectedScenario = selectWeightedFraudScenario();
            log.info("Generating fraud transaction with scenario: {}", selectedScenario.getDescription());

            TransactionWithMT103Event fraudTransactionEvent = generateFraudTransactionWithMT103(selectedScenario);

            // Add to history for pattern-based analysis
            addToHistory(fraudTransactionEvent.getTransaction());

            log.info("Generated fraud transaction: {} - Scenario: {}",
                    fraudTransactionEvent.getTransaction().getTransactionId(),
                    selectedScenario.getDescription());

            // Enhanced logging for fraud transaction details
            logFraudTransactionDetails(fraudTransactionEvent.getTransaction(), selectedScenario);

            transactionProducer.sendTransaction(fraudTransactionEvent);

        } catch (Exception e) {
            log.error("Error generating fraud transaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Selects a fraud scenario based on weighted probabilities
     */
    private FraudScenario selectWeightedFraudScenario() {
        int totalWeight = Arrays.stream(SCENARIO_WEIGHTS).sum();
        int randomValue = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (int i = 0; i < FRAUD_SCENARIOS.length; i++) {
            currentWeight += SCENARIO_WEIGHTS[i];
            if (randomValue < currentWeight) {
                return FRAUD_SCENARIOS[i];
            }
        }

        // Fallback to first scenario
        return FRAUD_SCENARIOS[0];
    }

    /**
     * Generates a fraud transaction with MT103 message
     */
    private TransactionWithMT103Event generateFraudTransactionWithMT103(FraudScenario scenario) {
        // Get recent transactions for pattern-based scenarios
        List<Transaction> recentTransactions = getRecentTransactions();

        // Generate the fraud transaction
        Transaction fraudTransaction = fraudTransactionFactory.createFraudTransaction(scenario, recentTransactions);

        // Create the event
        TransactionWithMT103Event event = new TransactionWithMT103Event();
        event.setTransaction(fraudTransaction);

        return event;
    }

    /**
     * Enhanced logging for fraud transaction details
     */
    private void logFraudTransactionDetails(Transaction transaction, FraudScenario scenario) {
        log.info("======================= FRAUD TRANSACTION =============================");
        log.info("Fraud Scenario: {} - {}", scenario.name(), scenario.getDescription());
        log.info("Transaction ID: {}", transaction.getTransactionId());
        log.info("Amount: {} {}", transaction.getAmount(), transaction.getCurrency());
        log.info("Timestamp: {}", transaction.getTimestamp());

        // Log party information
        log.info("From Bank: {} ({})", transaction.getFromBankName(), transaction.getFromBankSwift());
        log.info("From IBAN: {}", transaction.getFromIBAN());
        log.info("To Bank: {} ({})", transaction.getToBankName(), transaction.getToBankSwift());
        log.info("To IBAN: {}", transaction.getToIBAN());


        // Log scenario-specific details
        logScenarioSpecificDetails(transaction, scenario);

        log.info("===================================================================");
    }

    /**
     * Logs scenario-specific details
     */
    private void logScenarioSpecificDetails(Transaction transaction, FraudScenario scenario) {
        switch (scenario) {
            case HIGH_AMOUNT_THRESHOLD ->
                    log.info("High Amount Alert: {} exceeds threshold", transaction.getAmount());

            case OFF_HOURS_TRANSACTION ->
                    log.info("Off-Hours Alert: Transaction at {}", transaction.getTimestamp().toLocalTime());

            case SUSPICIOUS_REMITTANCE ->
                    log.info("Suspicious Remittance Alert: Contains suspicious keywords");

            case ROUND_AMOUNT_PATTERN ->
                    log.info("Round Amount Alert: Exact round amount {}", transaction.getAmount());

            case FREQUENT_SMALL_AMOUNTS ->
                    log.info("Small Amount Alert: Potential structuring behavior");

            case CROSS_BORDER_HIGH_RISK ->
                    log.info("High-Risk Country Alert: Destination country {}", transaction.getToCountryCode());

            case STRUCTURING_PATTERN ->
                    log.info("Structuring Alert: Amount just under reporting threshold");

            case CRYPTOCURRENCY_KEYWORDS ->
                    log.info("Cryptocurrency Alert: Contains crypto-related keywords");
        }
    }

    /**
     * Adds transaction to history and manages size
     */
    private void addToHistory(Transaction transaction) {
        transactionHistory.offer(transaction);

        // Remove oldest entries if history exceeds max size
        while (transactionHistory.size() > MAX_HISTORY_SIZE) {
            transactionHistory.poll();
        }

        log.debug("Transaction history size: {}", transactionHistory.size());
    }

    /**
     * Gets recent transactions for pattern analysis
     */
    private List<Transaction> getRecentTransactions() {
        return new ArrayList<>(transactionHistory);
    }

    /**
     * Manual trigger for generating specific fraud scenario (for testing)
     */
    public TransactionWithMT103Event generateSpecificFraudTransaction(FraudScenario scenario) {
        log.info("Manually generating fraud transaction for scenario: {}", scenario.getDescription());

        TransactionWithMT103Event event = generateFraudTransactionWithMT103(scenario);
        addToHistory(event.getTransaction());

        return event;
    }

    /**
     * Gets current transaction history size
     */
    public int getHistorySize() {
        return transactionHistory.size();
    }

    /**
     * Clears transaction history (for testing/maintenance)
     */
    public void clearHistory() {
        transactionHistory.clear();
        log.info("Transaction history cleared");
    }
}