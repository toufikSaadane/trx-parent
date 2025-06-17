package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.dto.AnalyticsDto;
import com.toufik.trxalertservice.dto.FraudStatisticsDto;
import com.toufik.trxalertservice.model.FraudAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class AnalyticsService {

    private final FraudFileWriterService fraudFileWriterService;
    private final List<FraudDetector> fraudDetectors;

    @Autowired
    public AnalyticsService(FraudFileWriterService fraudFileWriterService,
                            List<FraudDetector> fraudDetectors) {
        this.fraudFileWriterService = fraudFileWriterService;
        this.fraudDetectors = fraudDetectors;
    }

    public AnalyticsDto getAnalyticsData() {
        log.info("Generating analytics data");

        FraudStatisticsDto stats = fraudFileWriterService.getStatistics();

        return AnalyticsDto.builder()
                .transactionStats(buildTransactionStats(stats))
                .fraudStats(buildFraudStats(stats))
                .recentTransactions(buildRecentTransactions())
                .recentFraudAlerts(buildRecentFraudAlerts())
                .detectorStatuses(buildDetectorStatuses())
                .systemHealth(buildSystemHealth())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private AnalyticsDto.TransactionStats buildTransactionStats(FraudStatisticsDto stats) {
        return AnalyticsDto.TransactionStats.builder()
                .totalTransactions(stats.getTotalTransactionCount())
                .normalTransactions(stats.getNormalTransactionCount())
                .fraudulentTransactions(stats.getFraudulentTransactionCount())
                .fraudPercentage(stats.getFraudulentPercentage())
                .build();
    }

    private AnalyticsDto.FraudStats buildFraudStats(FraudStatisticsDto stats) {
        // These would typically come from a database or cache
        // For now, we'll generate mock data based on fraudulent transactions
        long totalAlerts = stats.getFraudulentTransactionCount() * 2; // Assume 2 alerts per fraudulent transaction on average

        return AnalyticsDto.FraudStats.builder()
                .totalAlerts(totalAlerts)
                .highRiskAlerts(totalAlerts * 30 / 100) // 30% high risk
                .mediumRiskAlerts(totalAlerts * 50 / 100) // 50% medium risk
                .lowRiskAlerts(totalAlerts * 20 / 100) // 20% low risk
                .averageRiskScore(stats.getFraudulentTransactionCount() > 0 ? 65.5 : 0.0)
                .build();
    }

    private List<AnalyticsDto.RecentTransactionDto> buildRecentTransactions() {
        List<AnalyticsDto.RecentTransactionDto> transactions = new ArrayList<>();
        Random random = new Random();

        // Generate 10 sample recent transactions
        String[] banks = {"Chase Bank", "Bank of America", "Wells Fargo", "Citibank", "HSBC", "Deutsche Bank"};
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD"};
        String[] statuses = {"PROCESSED", "RECEIVED", "VALIDATED", "FRAUD_SUSPECTED"};

        for (int i = 0; i < 10; i++) {
            transactions.add(AnalyticsDto.RecentTransactionDto.builder()
                    .transactionId("TXN-" + (1000 + i))
                    .amount(String.format("%.2f", 1000 + random.nextDouble() * 50000))
                    .currency(currencies[random.nextInt(currencies.length)])
                    .fromBank(banks[random.nextInt(banks.length)])
                    .toBank(banks[random.nextInt(banks.length)])
                    .status(statuses[random.nextInt(statuses.length)])
                    .isFraudulent(random.nextBoolean())
                    .riskScore(random.nextInt(100))
                    .timestamp(LocalDateTime.now().minusHours(random.nextInt(24)))
                    .build());
        }

        return transactions;
    }

    private List<AnalyticsDto.RecentFraudAlertDto> buildRecentFraudAlerts() {
        List<AnalyticsDto.RecentFraudAlertDto> alerts = new ArrayList<>();
        Random random = new Random();

        FraudAlert.FraudType[] fraudTypes = FraudAlert.FraudType.values();
        String[] alertStatuses = {"ACTIVE", "RESOLVED", "INVESTIGATING", "DISMISSED"};

        for (int i = 0; i < 15; i++) {
            FraudAlert.FraudType fraudType = fraudTypes[random.nextInt(fraudTypes.length)];

            alerts.add(AnalyticsDto.RecentFraudAlertDto.builder()
                    .alertId("ALERT-" + (2000 + i))
                    .transactionId("TXN-" + (1000 + random.nextInt(1000)))
                    .fraudType(fraudType.name())
                    .description(fraudType.getDescription())
                    .severity(1 + random.nextInt(10))
                    .status(alertStatuses[random.nextInt(alertStatuses.length)])
                    .timestamp(LocalDateTime.now().minusHours(random.nextInt(48)))
                    .build());
        }

        return alerts;
    }

    private List<AnalyticsDto.DetectorStatusDto> buildDetectorStatuses() {
        List<AnalyticsDto.DetectorStatusDto> statuses = new ArrayList<>();
        Random random = new Random();

        for (FraudDetector detector : fraudDetectors) {
            statuses.add(AnalyticsDto.DetectorStatusDto.builder()
                    .detectorName(detector.getDetectorName())
                    .isEnabled(detector.isEnabled())
                    .priority(detector.getPriority())
                    .alertsGenerated(random.nextInt(500))
                    .lastActivity(LocalDateTime.now().minusMinutes(random.nextInt(120)).toString())
                    .build());
        }

        return statuses;
    }

    private AnalyticsDto.SystemHealthDto buildSystemHealth() {
        int activeDetectors = (int) fraudDetectors.stream()
                .mapToLong(detector -> detector.isEnabled() ? 1 : 0)
                .sum();

        String healthStatus = activeDetectors == fraudDetectors.size() ? "HEALTHY" :
                activeDetectors > fraudDetectors.size() / 2 ? "WARNING" : "CRITICAL";

        return AnalyticsDto.SystemHealthDto.builder()
                .status(healthStatus)
                .activeDetectors(activeDetectors)
                .totalDetectors(fraudDetectors.size())
                .lastProcessedTransaction(LocalDateTime.now().minusMinutes(new Random().nextInt(60)))
                .uptimeStatus("OPERATIONAL")
                .build();
    }
}