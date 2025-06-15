package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.FraudScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final int MAX_HISTORY_SIZE = 100;

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
            log.info("======================= FRAUD TRANSACTION =============================");
            log.info("Fraud Scenario: {}", selectedScenario.getDescription());
            log.info("Transaction Details: {}", fraudTransactionEvent.getTransaction());
            log.info("Amount: {}", fraudTransactionEvent.getTransaction().getAmount());
            log.info("From IBAN: {}", fraudTransactionEvent.getTransaction().getFromIBAN());
            log.info("To IBAN: {}", fraudTransactionEvent.getTransaction().getToIBAN());
            log.info("Timestamp: {}", fraudTransactionEvent.getTransaction().getTimestamp());
            log.info("===================================================================");

            transactionProducer.sendTransaction(fraudTransactionEvent);

        } catch (Exception e) {
            log.error("Error generating fraud transaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates a complete fraud transaction with MT103 message
     */
    private TransactionWithMT103Event generateFraudTransactionWithMT103(FraudScenario scenario) {
        List<Transaction> recentTransactions = getRecentTransactions();
        Transaction fraudTransaction = fraudTransactionFactory.createFraudTransaction(scenario, recentTransactions);
        String mt103Content = fraudMT103MessageFormatter.formatToMT103(fraudTransaction);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(fraudTransaction);
        result.setMt103Content(mt103Content);

        return result;
    }

    /**
     * Selects a fraud scenario based on weighted probabilities
     */
    private FraudScenario selectWeightedFraudScenario() {
        List<FraudScenario> weightedScenarios = buildWeightedScenarioList();
        return weightedScenarios.get(random.nextInt(weightedScenarios.size()));
    }

    /**
     * Builds a weighted list of fraud scenarios based on their weights
     */
    private List<FraudScenario> buildWeightedScenarioList() {
        List<FraudScenario> weightedList = new ArrayList<>();

        for (FraudScenario scenario : FraudScenario.values()) {
            for (int i = 0; i < scenario.getWeight(); i++) {
                weightedList.add(scenario);
            }
        }

        log.debug("Built weighted scenario list with {} total entries", weightedList.size());
        return weightedList;
    }

    /**
     * Adds transaction to history and maintains max size
     */
    private void addToHistory(Transaction transaction) {
        transactionHistory.offer(transaction);

        // Remove oldest transactions if history exceeds max size
        while (transactionHistory.size() > MAX_HISTORY_SIZE) {
            transactionHistory.poll();
        }

        log.debug("Added transaction to history. Current history size: {}", transactionHistory.size());
    }

    /**
     * Gets recent transactions from history
     */
    private List<Transaction> getRecentTransactions() {
        return new ArrayList<>(transactionHistory);
    }

}