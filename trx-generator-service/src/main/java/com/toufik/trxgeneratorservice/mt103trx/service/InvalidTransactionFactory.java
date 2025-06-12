package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.InvalidScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Factory for creating invalid transactions with corrupted MT103 messages
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionFactory {

    private final TransactionGenerator transactionGenerator;
    private final MT103MessageCorruptor mt103MessageCorruptor;
    private final Random random = new Random();

    // Array of all available invalid scenarios
    private final InvalidScenario[] invalidScenarios = InvalidScenario.values();

    /**
     * Creates a transaction with an invalid MT103 message based on a random scenario
     */
    public TransactionWithMT103Event createInvalidTransaction() {
        // Generate a base transaction
        Transaction transaction = transactionGenerator.generateRandomTransaction();

        // Select random invalid scenario
        InvalidScenario scenario = selectRandomScenario();

        // Generate invalid MT103 content based on scenario
        String invalidMT103Content = mt103MessageCorruptor.generateInvalidMT103(transaction, scenario);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(transaction);
        result.setMt103Content(invalidMT103Content);

        log.warn("Created invalid transaction with scenario: {} for transaction: {}",
                scenario, transaction.getTransactionId());

        return result;
    }

    /**
     * Creates a transaction with an invalid MT103 message based on a specific scenario
     */
    public TransactionWithMT103Event createInvalidTransaction(InvalidScenario scenario) {
        Transaction transaction = transactionGenerator.generateRandomTransaction();
        String invalidMT103Content = mt103MessageCorruptor.generateInvalidMT103(transaction, scenario);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(transaction);
        result.setMt103Content(invalidMT103Content);

        log.warn("Created invalid transaction with specified scenario: {} for transaction: {}",
                scenario, transaction.getTransactionId());

        return result;
    }

    private InvalidScenario selectRandomScenario() {
        return invalidScenarios[random.nextInt(invalidScenarios.length)];
    }
}