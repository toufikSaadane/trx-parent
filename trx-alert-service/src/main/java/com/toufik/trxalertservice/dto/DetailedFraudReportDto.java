package com.toufik.trxalertservice.dto;

import com.toufik.trxalertservice.model.FraudDetectionResult;
import com.toufik.trxalertservice.model.Transaction;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DetailedFraudReportDto {
    private String transactionId;
    private Transaction transaction;
    private FraudDetectionResult fraudDetectionResult;
    private LocalDateTime reportGeneratedAt;
}