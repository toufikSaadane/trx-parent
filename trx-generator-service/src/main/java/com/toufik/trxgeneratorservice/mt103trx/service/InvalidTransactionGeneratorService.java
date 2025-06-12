package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating and sending invalid transactions at scheduled intervals
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionGeneratorService {

    private final InvalidTransactionFactory invalidTransactionFactory;
    private final TransactionProducer transactionProducer;

    /**
     * Generates invalid transactions every 15 seconds
     */
    @Scheduled(fixedRate = 15000)
    public void generateAndSendInvalidTransaction() {
        try {
            TransactionWithMT103Event invalidTransactionEvent = invalidTransactionFactory.createInvalidTransaction();

            logInvalidTransactionDetails(invalidTransactionEvent);

            transactionProducer.sendTransaction(invalidTransactionEvent);

            log.info("Successfully sent invalid transaction: {}",
                    invalidTransactionEvent.getTransaction().getTransactionId());

        } catch (Exception e) {
            log.error("Error generating invalid transaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates an invalid transaction on demand
     */
    public TransactionWithMT103Event generateInvalidTransactionOnDemand() {
        try {
            TransactionWithMT103Event invalidTransactionEvent = invalidTransactionFactory.createInvalidTransaction();
            logInvalidTransactionDetails(invalidTransactionEvent);
            return invalidTransactionEvent;
        } catch (Exception e) {
            log.error("Error generating invalid transaction on demand: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate invalid transaction", e);
        }
    }

    private void logInvalidTransactionDetails(TransactionWithMT103Event invalidTransactionEvent) {
        log.warn("Generated INVALID transaction: {}",
                invalidTransactionEvent.getTransaction().getTransactionId());
        log.warn("======================= INVALID TRANSACTION =============================");
        log.warn("Invalid Transaction Details: {}", invalidTransactionEvent.getTransaction());

        String mt103Content = invalidTransactionEvent.getMt103Content();
        String preview = mt103Content.substring(0, Math.min(200, mt103Content.length()));
        log.warn("Invalid MT103 Content Preview: {}", preview);

        if (mt103Content.length() > 200) {
            log.warn("MT103 Content Length: {} characters (truncated for logging)", mt103Content.length());
        }
    }
}