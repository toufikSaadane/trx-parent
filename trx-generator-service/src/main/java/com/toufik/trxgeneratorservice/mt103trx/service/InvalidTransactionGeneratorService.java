package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionGeneratorService {

    private final InvalidTransactionFactory invalidTransactionFactory;
    private final TransactionProducer transactionProducer;

    @Autowired
    private TransactionSaveService transactionSaveService; // Added

    @Scheduled(fixedRate = 15000)
    public void generateAndSendInvalidTransaction() {
        try {
            TransactionWithMT103Event invalidTransactionEvent = invalidTransactionFactory.createInvalidTransaction();

            // Save to MongoDB with invalid reason
            transactionSaveService.saveInvalidTransaction(
                    invalidTransactionEvent.getTransaction(),
                    invalidTransactionEvent.getMt103Content(),
                    "CORRUPTED_MT103" // You can determine the actual invalid scenario
            );

            logInvalidTransactionDetails(invalidTransactionEvent);
            transactionProducer.sendTransaction(invalidTransactionEvent);

            log.info("Successfully sent invalid transaction: {}",
                    invalidTransactionEvent.getTransaction().getTransactionId());

        } catch (Exception e) {
            log.error("Error generating invalid transaction: {}", e.getMessage(), e);
        }
    }

    private void logInvalidTransactionDetails(TransactionWithMT103Event invalidTransactionEvent) {
        log.warn("Generated INVALID transaction: {}", invalidTransactionEvent.getTransaction().getTransactionId());
    }
}