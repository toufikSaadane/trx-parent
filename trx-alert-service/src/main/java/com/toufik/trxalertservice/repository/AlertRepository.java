package com.toufik.trxalertservice.repository;

import com.toufik.trxalertservice.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends MongoRepository<TransactionEntity, String> {


    List<TransactionEntity> findByFraudulent(boolean fraudulent);
    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    List<TransactionEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    @Query("{'fraudulent': true, 'timestamp': {$gte: ?0, $lte: ?1}}")
    List<TransactionEntity> findFraudulentTransactionsBetween(LocalDateTime start, LocalDateTime end);
}