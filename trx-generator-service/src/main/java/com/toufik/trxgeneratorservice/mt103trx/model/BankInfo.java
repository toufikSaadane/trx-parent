package com.toufik.trxgeneratorservice.mt103trx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankInfo {
    private String swiftCode;
    private String countryCode;
    private String bankName;
    private String routingNumber;
    private String currencyCode;
    private String ibanPrefix;
    private Integer ibanLength;
}