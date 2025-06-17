package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.dto.DetailedFraudReportDto;
import com.toufik.trxalertservice.dto.TransactionRecordDto;
import com.toufik.trxalertservice.model.FraudAlert;
import com.toufik.trxalertservice.model.FraudDetectionResult;
import com.toufik.trxalertservice.model.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class FraudMapper {

    public TransactionRecordDto toTransactionRecord(Transaction transaction, FraudDetectionResult fraudResult) {
        return TransactionRecordDto.builder()
                .transactionId(transaction.getTransactionId())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .fromBankName(transaction.getFromBankName())
                .toBankName(transaction.getToBankName())
                .fromCountryCode(transaction.getFromCountryCode())
                .toCountryCode(transaction.getToCountryCode())
                .timestamp(transaction.getTimestamp())
                .status(transaction.getStatus())
//                .remittanceInfo(transaction.getRemittanceInfo())
                .isFraudulent(fraudResult.isFraudulent())
                .riskScore(fraudResult.getRiskScore())
                .riskLevel(fraudResult.getRiskLevel())
                .alertCount(getAlertCount(fraudResult))
                .alertTypes(getAlertTypes(fraudResult))
                .detectionTimestamp(fraudResult.getDetectionTimestamp())
                .processedAt(LocalDateTime.now())
                .build();
    }

    public DetailedFraudReportDto toDetailedReport(Transaction transaction, FraudDetectionResult fraudResult) {
        return DetailedFraudReportDto.builder()
                .transactionId(transaction.getTransactionId())
                .transaction(transaction)
                .fraudDetectionResult(fraudResult)
                .reportGeneratedAt(LocalDateTime.now())
                .build();
    }

    private int getAlertCount(FraudDetectionResult fraudResult) {
        return fraudResult.getAlerts() != null ? fraudResult.getAlerts().size() : 0;
    }

    private List<String> getAlertTypes(FraudDetectionResult fraudResult) {
        if (fraudResult.getAlerts() == null) {
            return Collections.emptyList();
        }

        return fraudResult.getAlerts().stream()
                .map(FraudAlert::getFraudType)
                .map(Enum::name)
                .toList();
    }
}