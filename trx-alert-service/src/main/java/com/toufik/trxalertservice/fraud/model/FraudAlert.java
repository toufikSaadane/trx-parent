package com.toufik.trxalertservice.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FraudAlert {
    private String transactionId;
    private String ruleName;
    private String description;
    private String severity; // HIGH, MEDIUM, LOW
    private LocalDateTime alertTime;
    private String details;
}