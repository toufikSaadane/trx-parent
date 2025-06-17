package com.toufik.trxalertservice.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class FraudAlert {
    private String alertId;
    private FraudType fraudType;
    private String description;
    private int severity; // 1-10
    private String details;
    private LocalDateTime timestamp;
    private AlertStatus status;

    public enum FraudType {
        HIGH_AMOUNT_THRESHOLD("High Amount Transaction"),
        OFF_HOURS_TRANSACTION("Off-Hours Transaction"),
        SUSPICIOUS_REMITTANCE("Suspicious Remittance Information"),
        ROUND_AMOUNT_PATTERN("Round Amount Pattern"),
        FREQUENT_SMALL_AMOUNTS("Frequent Small Amounts"),
        CROSS_BORDER_HIGH_RISK("Cross-Border High Risk"),
        STRUCTURING_PATTERN("Structuring Pattern"),
        CRYPTOCURRENCY_KEYWORDS("Cryptocurrency Keywords");

        private final String description;

        FraudType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum AlertStatus {
        ACTIVE, RESOLVED, FALSE_POSITIVE, UNDER_INVESTIGATION
    }
}