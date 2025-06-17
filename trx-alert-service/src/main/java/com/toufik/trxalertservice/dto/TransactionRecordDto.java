package com.toufik.trxalertservice.dto;

import com.toufik.trxalertservice.model.FraudDetectionResult;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransactionRecordDto {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String fromBankName;
    private String toBankName;
    private String fromCountryCode;
    private String toCountryCode;
    private LocalDateTime timestamp;
    private String status;
    private boolean isFraudulent;
    private int riskScore;
    private FraudDetectionResult.RiskLevel riskLevel;
    private int alertCount;
    private List<String> alertTypes;
    private LocalDateTime detectionTimestamp;
    private LocalDateTime processedAt;
}