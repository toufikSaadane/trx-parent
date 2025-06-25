package com.toufik.trxgeneratorservice.mt103trx.repository;

import com.toufik.trxgeneratorservice.mt103trx.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<TransactionEntity, String> {

    // Find by transaction type
    List<TransactionEntity> findByTransactionType(String transactionType);

    // Find by date range
    List<TransactionEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Find by transaction type and date range
    List<TransactionEntity> findByTransactionTypeAndTimestampBetween(
            String transactionType, LocalDateTime start, LocalDateTime end);

    // Find by country codes
    List<TransactionEntity> findByFromCountryCodeOrToCountryCode(String fromCountry, String toCountry);

    // Count by transaction type
    long countByTransactionType(String transactionType);

    // Find recent transactions
    @Query(value = "{}", sort = "{ 'timestamp': -1 }")
    List<TransactionEntity> findRecentTransactions();
}