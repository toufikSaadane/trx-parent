package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import com.toufik.trxgeneratorservice.mt103trx.util.AmountGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransactionGeneratorService extends BaseTransactionFactory {

    private final TransactionProducer transactionProducer;
    private final MT103MessageFormatter mt103MessageFormatter;
    private final TransactionSaveService transactionSaveService;

    public TransactionGeneratorService(TransactionProducer transactionProducer,
                                       @Qualifier("MT103MessageFormatter") MT103MessageFormatter mt103MessageFormatter,
                                       TransactionSaveService transactionSaveService) {
        this.transactionProducer = transactionProducer;
        this.mt103MessageFormatter = mt103MessageFormatter;
        this.transactionSaveService = transactionSaveService;
    }

    @Scheduled(fixedRate = 5000)
    public void generateAndSendTransaction() {
        try {
            TransactionWithMT103Event transactionWithMT103Event = generateRandomTransactionWithMT103();

            // Save the complete transaction to MongoDB
            transactionSaveService.saveTransaction(
                    transactionWithMT103Event.getTransaction(),
                    transactionWithMT103Event.getMt103Content()
            );

            // Send to Kafka
            transactionProducer.sendTransaction(transactionWithMT103Event);

            // Log transaction details
            logValidTransactionDetails(transactionWithMT103Event);
        } catch (Exception e) {
            log.error("Error generating transaction: {}", e.getMessage(), e);
        }
    }

    private TransactionWithMT103Event generateRandomTransactionWithMT103() {
        Transaction transaction = createBaseTransaction();
        String mt103Content = mt103MessageFormatter.formatToMT103(transaction);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(transaction);
        result.setMt103Content(mt103Content);

        return result;
    }

    @Override
    protected BigDecimal generateRandomAmount() {
        return AmountGenerator.generateMedium();
    }

    private void logValidTransactionDetails(TransactionWithMT103Event transactionEvent) {
        Transaction transaction = transactionEvent.getTransaction();
        log.info("Generated NORMAL transaction: {} | Amount: {} {} | From: {} -> To: {}",
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getFromBankSwift(),
                transaction.getToBankSwift());
    }
}