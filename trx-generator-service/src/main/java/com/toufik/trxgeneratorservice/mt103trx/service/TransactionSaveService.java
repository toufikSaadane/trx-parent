package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.entity.TransactionEntity;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class TransactionSaveService {

    private final TransactionRepository transactionRepository;
    private final Random random = new Random();

    public TransactionSaveService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void saveTransaction(Transaction transaction, String mt103Content) {
        try {
            TransactionEntity entity = new TransactionEntity();

            // Copy transaction fields to entity
            // ... set all the fields from transaction to entity ...

            entity.setMt103Content(mt103Content);

            // Randomize status
            String[] statuses = {"NORMAL", "FRAUD", "INVALID"};
            String status = statuses[random.nextInt(statuses.length)];
            entity.setTransactionType(status);

            switch (status) {
                case "NORMAL":
                    entity.setRiskScore(0.1);
                    break;
                case "FRAUD":
                    entity.setFraudPattern("PATTERN_" + random.nextInt(5));
                    entity.setRiskScore(0.8);
                    break;
                case "INVALID":
                    entity.setInvalidReason("REASON_" + random.nextInt(5));
                    entity.setRiskScore(0.0);
                    entity.setIsProcessed(false);
                    break;
            }

            TransactionEntity saved = transactionRepository.save(entity);
            log.info("Saved {} transaction: {}", status, saved.getTransactionId());
        } catch (Exception e) {
            log.error("Error saving transaction: {}", e.getMessage(), e);
        }
    }
}