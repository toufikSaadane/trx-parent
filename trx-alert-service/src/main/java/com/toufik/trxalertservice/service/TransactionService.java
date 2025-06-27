package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.entity.TransactionEntity;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import com.toufik.trxalertservice.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final AlertRepository transactionRepository;

    public TransactionEntity saveTransaction(TransactionWithMT103Event event, List<FraudAlert> fraudAlerts) {
        Transaction transaction = event.getTransaction();

        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(transaction.getTransactionId());
        entity.setFromAccount(transaction.getFromAccount());
        entity.setToAccount(transaction.getToAccount());
        entity.setAmount(transaction.getAmount());
        entity.setCurrency(transaction.getCurrency());
        entity.setFromBankSwift(transaction.getFromBankSwift());
        entity.setToBankSwift(transaction.getToBankSwift());
        entity.setFromBankName(transaction.getFromBankName());
        entity.setToBankName(transaction.getToBankName());
        entity.setTimestamp(transaction.getTimestamp());
        entity.setStatus(transaction.getStatus());
        entity.setFromIBAN(transaction.getFromIBAN());
        entity.setToIBAN(transaction.getToIBAN());
        entity.setFromCountryCode(transaction.getFromCountryCode());
        entity.setToCountryCode(transaction.getToCountryCode());
        entity.setMt103Content(event.getMt103Content());

        entity.setFraudulent(!fraudAlerts.isEmpty());
        entity.setFraudReasons(fraudAlerts.stream()
                .map(alert -> alert.getRuleName() + ": " + alert.getDescription())
                .collect(Collectors.toList()));
        entity.setProcessedAt(LocalDateTime.now());

        TransactionEntity saved = transactionRepository.save(entity);

        log.info("Saved transaction {} to MongoDB - Fraudulent: {}, Fraud reasons: {}",
                entity.getTransactionId(), entity.isFraudulent(), entity.getFraudReasons());

        return saved;
    }

    public List<TransactionEntity> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<TransactionEntity> getFraudulentTransactions() {
        return transactionRepository.findByFraudulent(true);
    }

    public List<TransactionEntity> getFraudulentTransactionsBetween(LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findFraudulentTransactionsBetween(start, end);
    }
}