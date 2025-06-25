package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FraudTransactionGeneratorService {

    @Autowired
    private FraudTransactionFactory fraudTransactionFactory;

    @Autowired
    private TransactionProducer transactionProducer;

    @Autowired
    private FraudMT103MessageFormatter fraudMT103MessageFormatter;

    @Autowired
    private TransactionSaveService transactionSaveService; // Added

    @Scheduled(fixedRate = 10000)
    public void generateAndSendFraudTransaction() {
        try {
            TransactionWithMT103Event fraudTransactionEvent = generateFraudTransactionWithMT103();

            // Save to MongoDB with fraud pattern
            transactionSaveService.saveFraudTransaction(
                    fraudTransactionEvent.getTransaction(),
                    fraudTransactionEvent.getMt103Content(),
                    "RANDOM_PATTERN" // You can determine the actual pattern from FraudTransactionFactory
            );

            logFraudTransactionDetails(fraudTransactionEvent.getTransaction());
            transactionProducer.sendTransaction(fraudTransactionEvent);

        } catch (Exception e) {
            log.error("Error generating fraud transaction: {}", e.getMessage(), e);
        }
    }

    private TransactionWithMT103Event generateFraudTransactionWithMT103() {
        Transaction fraudTransaction = fraudTransactionFactory.createFraudTransaction();
        String mt103Content = fraudMT103MessageFormatter.formatToMT103(fraudTransaction);
        TransactionWithMT103Event event = new TransactionWithMT103Event();
        event.setTransaction(fraudTransaction);
        event.setMt103Content(mt103Content);
        return event;
    }

    private void logFraudTransactionDetails(Transaction transaction) {
        log.info("======================= FRAUD TRANSACTION =============================");
        log.info("Transaction ID: {}", transaction.getTransactionId());
        log.info("Amount: {} {}", transaction.getAmount(), transaction.getCurrency());
        log.info("===================================================================");
    }
}