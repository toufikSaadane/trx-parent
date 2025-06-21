package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.FraudScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class FraudTransactionGeneratorService {

    @Autowired
    private FraudTransactionFactory fraudTransactionFactory;

    @Autowired
    private TransactionProducer transactionProducer;

    private final Random random = new Random();

    @Scheduled(fixedRate = 10000)
    public void generateAndSendFraudTransaction() {
        try {
            FraudScenario selectedScenario = selectFraudScenario();
            log.info("Generating fraud transaction with scenario: {}", selectedScenario.getDescription());
            TransactionWithMT103Event fraudTransactionEvent = generateFraudTransactionWithMT103(selectedScenario);
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
    private FraudScenario selectFraudScenario() {
        FraudScenario[] scenarios = FraudScenario.values();
        return scenarios[random.nextInt(scenarios.length)];
    }

    /**
     * Generates a fraud transaction with MT103 message
     */
    private TransactionWithMT103Event generateFraudTransactionWithMT103(FraudScenario scenario) {

        // Generate the fraud transaction
        Transaction fraudTransaction = fraudTransactionFactory.createFraudTransaction(scenario);

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

        log.info("===================================================================");
    }

}