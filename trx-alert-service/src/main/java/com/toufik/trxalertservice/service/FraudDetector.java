package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;

public interface FraudDetector {
    FraudAlert detect(Transaction transaction);
    boolean isEnabled();
    String getDetectorName();
    int getPriority();
}
