package com.toufik.trxgeneratorservice.mt103trx.repository;

import com.toufik.trxgeneratorservice.mt103trx.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<TransactionEntity, String> {

    List<TransactionEntity> findByTransactionType(String transactionType);
    List<TransactionEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    long countByTransactionType(String transactionType);
    @Query(value = "{}", sort = "{ 'timestamp': -1 }")
    List<TransactionEntity> findRecentTransactions();
}