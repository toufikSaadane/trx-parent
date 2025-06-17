package com.toufik.trxalertservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AnalyticsDto {

    // Transaction Statistics
    private TransactionStats transactionStats;

    // Fraud Detection Statistics
    private FraudStats fraudStats;

    // Recent Transactions
    private List<RecentTransactionDto> recentTransactions;

    // Recent Fraud Alerts
    private List<RecentFraudAlertDto> recentFraudAlerts;

    // Detector Status
    private List<DetectorStatusDto> detectorStatuses;

    // System Health
    private SystemHealthDto systemHealth;

    // Last Updated
    private LocalDateTime lastUpdated;

    @Data
    @Builder
    public static class TransactionStats {
        private long totalTransactions;
        private long normalTransactions;
        private long fraudulentTransactions;
        private double fraudPercentage;
    }

    @Data
    @Builder
    public static class FraudStats {
        private long totalAlerts;
        private long highRiskAlerts;
        private long mediumRiskAlerts;
        private long lowRiskAlerts;
        private double averageRiskScore;
    }

    @Data
    @Builder
    public static class RecentTransactionDto {
        private String transactionId;
        private String amount;
        private String currency;
        private String fromBank;
        private String toBank;
        private String status;
        private boolean isFraudulent;
        private int riskScore;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class RecentFraudAlertDto {
        private String alertId;
        private String transactionId;
        private String fraudType;
        private String description;
        private int severity;
        private String status;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class DetectorStatusDto {
        private String detectorName;
        private boolean isEnabled;
        private int priority;
        private long alertsGenerated;
        private String lastActivity;
    }

    @Data
    @Builder
    public static class SystemHealthDto {
        private String status;
        private int activeDetectors;
        private int totalDetectors;
        private LocalDateTime lastProcessedTransaction;
        private String uptimeStatus;
    }
}