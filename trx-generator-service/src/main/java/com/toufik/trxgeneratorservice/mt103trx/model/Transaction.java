package com.toufik.trxgeneratorservice.mt103trx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
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

    public Transaction(String transactionId, String fromAccount, String toAccount,
                       BigDecimal amount, String currency, String fromBankSwift,
                       String toBankSwift, String fromBankName, String toBankName,
                       LocalDateTime timestamp, String status) {
        this.transactionId = transactionId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.fromBankSwift = fromBankSwift;
        this.toBankSwift = toBankSwift;
        this.fromBankName = fromBankName;
        this.toBankName = toBankName;
        this.timestamp = timestamp;
        this.status = status;
        this.fromCountryCode = extractCountryCode(fromBankSwift);
        this.toCountryCode = extractCountryCode(toBankSwift);
    }

    private String extractCountryCode(String swiftCode) {
        if (swiftCode != null && swiftCode.length() >= 6) {
            return swiftCode.substring(4, 6);
        }
        return null;
    }

    public boolean isCrossBorder() {
        return fromCountryCode != null && toCountryCode != null &&
                !fromCountryCode.equals(toCountryCode);
    }
}