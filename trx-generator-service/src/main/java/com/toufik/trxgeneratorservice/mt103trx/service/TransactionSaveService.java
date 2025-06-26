package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.entity.TransactionEntity;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionSaveService {

    private final TransactionRepository transactionRepository;
    private final ModelMapper modelMapper;

    public TransactionSaveService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
        this.modelMapper = new ModelMapper();
    }

    public void saveTransaction(Transaction transaction, String mt103Content) {
        try {
            TransactionEntity entity = modelMapper.map(transaction, TransactionEntity.class);

            entity.setMt103Content(mt103Content);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setIsProcessed(false);
            entity.setTransactionType("NORMAL");
            entity.setRiskScore(0.1);

            TransactionEntity saved = transactionRepository.save(entity);
            log.info("Saved NORMAL transaction: {}", saved.getTransactionId());
        } catch (Exception e) {
            log.error("Error saving transaction: {}", e.getMessage(), e);
        }
    }

    public void saveFraudTransaction(Transaction transaction, String mt103Content, String fraudPattern) {
        try {
            TransactionEntity entity = modelMapper.map(transaction, TransactionEntity.class);
            entity.setMt103Content(mt103Content);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setIsProcessed(false);
            entity.setTransactionType("FRAUD");
            entity.setFraudPattern(fraudPattern);
            entity.setRiskScore(0.8);

            TransactionEntity saved = transactionRepository.save(entity);
            log.info("Saved FRAUD transaction: {} with pattern: {}", saved.getTransactionId(), fraudPattern);
        } catch (Exception e) {
            log.error("Error saving fraud transaction: {}", e.getMessage(), e);
        }
    }

    public void saveInvalidTransaction(Transaction transaction, String mt103Content, String invalidReason) {
        try {
            TransactionEntity entity = modelMapper.map(transaction, TransactionEntity.class);

            entity.setMt103Content(mt103Content);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setIsProcessed(false);
            entity.setTransactionType("INVALID");
            entity.setInvalidReason(invalidReason);
            entity.setRiskScore(0.0);

            TransactionEntity saved = transactionRepository.save(entity);
            log.info("Saved INVALID transaction: {} with reason: {}", saved.getTransactionId(), invalidReason);
        } catch (Exception e) {
            log.error("Error saving invalid transaction: {}", e.getMessage(), e);
        }
    }
}