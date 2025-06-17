package com.toufik.trxalertservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FraudStatisticsDto {
    private long normalTransactionCount;
    private long fraudulentTransactionCount;
    private long totalTransactionCount;
    private double fraudulentPercentage;
    private LocalDateTime lastUpdated;
}