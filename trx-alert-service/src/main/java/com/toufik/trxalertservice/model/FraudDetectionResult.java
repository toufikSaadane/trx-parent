package com.toufik.trxalertservice.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class FraudDetectionResult {
    private String transactionId;
    private boolean isFraudulent;
    private int riskScore; // 0-100
    private RiskLevel riskLevel;
    private List<FraudAlert> alerts;
    private Map<String, Object> additionalData;
    private LocalDateTime detectionTimestamp;

    public enum RiskLevel {
        LOW(0, 30),
        MEDIUM(31, 70),
        HIGH(71, 90),
        CRITICAL(91, 100);

        private final int minScore;
        private final int maxScore;

        RiskLevel(int minScore, int maxScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
        }

        public static RiskLevel fromScore(int score) {
            for (RiskLevel level : values()) {
                if (score >= level.minScore && score <= level.maxScore) {
                    return level;
                }
            }
            return LOW;
        }
    }
}