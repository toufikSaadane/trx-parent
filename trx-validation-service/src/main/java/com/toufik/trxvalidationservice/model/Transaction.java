package com.toufik.trxvalidationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Document(collection = "transactions_validation")
public class Transaction {
    @Id
    private String id;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("fromAccount")
    private String fromAccount;

    @JsonProperty("toAccount")
    private String toAccount;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("fromBankSwift")
    private String fromBankSwift;

    @JsonProperty("toBankSwift")
    private String toBankSwift;

    @JsonProperty("fromBankName")
    private String fromBankName;

    @JsonProperty("toBankName")
    private String toBankName;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("status")
    private String status;

    @JsonProperty("fromIBAN")
    private String fromIBAN;

    @JsonProperty("toIBAN")
    private String toIBAN;

    @JsonProperty("fromCountryCode")
    private String fromCountryCode;

    @JsonProperty("toCountryCode")
    private String toCountryCode;

    // New fields for validation
    private boolean isValid;
    private String validationReason;
    private LocalDateTime processedAt;
}