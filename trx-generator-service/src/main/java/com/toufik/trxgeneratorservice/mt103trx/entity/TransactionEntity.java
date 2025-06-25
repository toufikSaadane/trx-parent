package com.toufik.trxgeneratorservice.mt103trx.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@Data
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

    private String transactionType;
    private String fraudPattern;
    private String invalidReason;
    private Double riskScore;
    private Boolean isProcessed = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}