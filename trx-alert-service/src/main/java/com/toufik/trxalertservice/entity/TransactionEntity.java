package com.toufik.trxalertservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transactions")
public class TransactionEntity {
    @Id
    private String id;

    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String fromBankSwift;
    private String toBankSwift;
    private String fromBankName;
    private String toBankName;
    private LocalDateTime timestamp;
    private String status;
    private String fromIBAN;
    private String toIBAN;
    private String fromCountryCode;
    private String toCountryCode;
    private String mt103Content;

    private boolean fraudulent;
    private List<String> fraudReasons;
    private LocalDateTime processedAt;
}