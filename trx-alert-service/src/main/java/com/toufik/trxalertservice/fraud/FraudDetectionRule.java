package com.toufik.trxalertservice.fraud;

import com.toufik.trxalertservice.model.TransactionWithMT103Event;

public interface FraudDetectionRule {
    boolean isSuspicious(TransactionWithMT103Event event);
    String getRuleName();
    String getDescription();
}