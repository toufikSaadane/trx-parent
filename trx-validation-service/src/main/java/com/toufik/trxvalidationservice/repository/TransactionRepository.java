package com.toufik.trxvalidationservice.repository;

import com.toufik.trxvalidationservice.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByIsValid(boolean isValid);
}