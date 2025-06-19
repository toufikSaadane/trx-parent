package com.toufik.trxalertservice.fraud.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class FraudDetectionMetricsService {

    private final AtomicLong totalTransactionsProcessed = new AtomicLong(0);
    private final AtomicLong structuringAlertsGenerated = new AtomicLong(0);
    private final AtomicLong highRiskCountryAlertsGenerated = new AtomicLong(0);
    private final AtomicLong thresholdAvoidanceAlertsGenerated = new AtomicLong(0);

    public void recordTransactionProcessed() {
        long total = totalTransactionsProcessed.incrementAndGet();
        if (total % 100 == 0) {
            log.info("Fraud Detection Metrics - Total transactions processed: {}", total);
        }
    }

    public void recordStructuringAlert() {
        long count = structuringAlertsGenerated.incrementAndGet();
        log.info("Structuring alert generated. Total count: {}", count);
    }

    public void recordHighRiskCountryAlert() {
        long count = highRiskCountryAlertsGenerated.incrementAndGet();
        log.info("High-risk country alert generated. Total count: {}", count);
    }

    public void recordThresholdAvoidanceAlert() {
        long count = thresholdAvoidanceAlertsGenerated.incrementAndGet();
        log.info("Threshold avoidance alert generated. Total count: {}", count);
    }

    public void logMetricsSummary() {
        log.info("=================== FRAUD DETECTION METRICS SUMMARY ===================");
        log.info("Total Transactions Processed: {}", totalTransactionsProcessed.get());
        log.info("Structuring Alerts: {}", structuringAlertsGenerated.get());
        log.info("High-Risk Country Alerts: {}", highRiskCountryAlertsGenerated.get());
        log.info("Threshold Avoidance Alerts: {}", thresholdAvoidanceAlertsGenerated.get());
        log.info("=====================================================================");
    }
}