package com.toufik.trxalertservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.model.FraudAlertReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class FraudAlertReportService {

    private final ObjectMapper objectMapper;
    private static final String REPORTS_DIR = "fraud-reports";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public FraudAlertReportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        createReportsDirectory();
    }

    public void writeAlertsToJsonFile(List<FraudAlert> alerts, String transactionId) {
        if (alerts.isEmpty()) {
            log.debug("No alerts to write for transaction: {}", transactionId);
            return;
        }

        try {
            FraudAlertReport report = createReport(alerts, transactionId);
            String fileName = generateFileName(transactionId);
            File file = new File(REPORTS_DIR + "/" + fileName);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, report);

            log.info("Fraud alert report written to: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write fraud alert report for transaction: {}", transactionId, e);
        }
    }

    private FraudAlertReport createReport(List<FraudAlert> alerts, String transactionId) {
        FraudAlertReport report = new FraudAlertReport();
        report.setTransactionId(transactionId);
        report.setReportGeneratedAt(LocalDateTime.now());
        report.setTotalAlerts(alerts.size());
        report.setAlerts(alerts);
        return report;
    }

    private String generateFileName(String transactionId) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return String.format("fraud-alert_%s_%s.json", transactionId, timestamp);
    }

    private void createReportsDirectory() {
        File dir = new File(REPORTS_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("Created fraud reports directory: {}", dir.getAbsolutePath());
            } else {
                log.error("Failed to create fraud reports directory: {}", dir.getAbsolutePath());
            }
        }
    }
}