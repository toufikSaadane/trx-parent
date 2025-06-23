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

    public TransactionGeneratorService(TransactionProducer transactionProducer,
                                       @Qualifier("MT103MessageFormatter") MT103MessageFormatter mt103MessageFormatter) {
        this.transactionProducer = transactionProducer;
        this.mt103MessageFormatter = mt103MessageFormatter;
    }

    @Scheduled(fixedRate = 10000)
    public void generateAndSendTransaction() {
        try {
            TransactionWithMT103Event transactionWithMT103Event = generateRandomTransactionWithMT103();
            transactionProducer.sendTransaction(transactionWithMT103Event);
            logValidTransactionDetails(transactionWithMT103Event);
        } catch (Exception e) {
            log.error("Error generating transaction: {}", e.getMessage(), e);
        }
    }

    public Transaction createTransaction() {
        return createBaseTransaction();
    }

    public Transaction generateRandomTransaction() {
        return createTransaction();
    }

    private TransactionWithMT103Event generateRandomTransactionWithMT103() {
        Transaction transaction = generateRandomTransaction();
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

    private void logValidTransactionDetails(TransactionWithMT103Event invalidTransactionEvent) {
        log.warn("Generated INVALID transaction: {}",
                invalidTransactionEvent.getTransaction().getTransactionId());
        log.warn("======================= VALID TRANSACTION =============================");
        log.warn("Valid Transaction Details: {}", invalidTransactionEvent.getTransaction());

        String mt103Content = invalidTransactionEvent.getMt103Content();
        log.warn("Invalid MT103 Content Preview: {}", mt103Content);

        if (mt103Content.length() > 200) {
            log.warn("MT103 Content Length: {} characters (truncated for logging)", mt103Content.length());
        }
    }
}