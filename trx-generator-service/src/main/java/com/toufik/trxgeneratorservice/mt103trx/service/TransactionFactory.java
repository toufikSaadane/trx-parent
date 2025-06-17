package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;

/**
 * Interface for transaction factory implementations
 */
public interface TransactionFactory {

    /**
     * Creates a new transaction
     * @return Transaction instance
     */
    Transaction createTransaction();
}