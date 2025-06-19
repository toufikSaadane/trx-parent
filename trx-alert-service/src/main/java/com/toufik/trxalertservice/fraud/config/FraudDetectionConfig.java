package com.toufik.trxalertservice.fraud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "fraud.detection")
@Data
public class FraudDetectionConfig {
    private StructuringConfig structuring = new StructuringConfig();
    private ThresholdConfig threshold = new ThresholdConfig();
    private List<String> highRiskCountries = List.of("AF", "IR", "KP", "MM", "SY", "YE");

    @Data
    public static class StructuringConfig {
        private BigDecimal amountThreshold = new BigDecimal("1000.00");
        private int maxTransactions = 5;
        private int timeWindowMinutes = 60;
    }

    @Data
    public static class ThresholdConfig {
        private BigDecimal reportingThreshold = new BigDecimal("10000.00");
        private BigDecimal suspiciousAmount = new BigDecimal("9999.99");
        private BigDecimal rangeStart = new BigDecimal("9900.00");
    }
}