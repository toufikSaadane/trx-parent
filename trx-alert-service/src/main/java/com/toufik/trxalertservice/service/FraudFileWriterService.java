package com.toufik.trxalertservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toufik.trxalertservice.dto.DetailedFraudReportDto;
import com.toufik.trxalertservice.dto.FraudStatisticsDto;
import com.toufik.trxalertservice.dto.TransactionRecordDto;
import com.toufik.trxalertservice.model.FraudDetectionResult;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.service.FraudMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class FraudFileWriterService {

    @Value("${fraud.output.directory:src/main/resources/fraud-detection}")
    private String outputDirectory;

    @Value("${fraud.output.normal-transactions-file:normal_transactions.json}")
    private String normalTransactionsFile;

    @Value("${fraud.output.fraudulent-transactions-file:fraudulent_transactions.json}")
    private String fraudulentTransactionsFile;

    @Value("${fraud.output.detailed-reports:true}")
    private boolean enableDetailedReports;

    private final ObjectMapper objectMapper;
    private final FraudMapper fraudMapper;
    private final AtomicLong normalTransactionCount = new AtomicLong(0);
    private final AtomicLong fraudulentTransactionCount = new AtomicLong(0);

    public FraudFileWriterService(FraudMapper fraudMapper) {
        this.fraudMapper = fraudMapper;
        this.objectMapper = createObjectMapper();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    @PostConstruct
    public void init() {
        try {
            createOutputDirectories();
            initializeTransactionFiles();
            log.info("FraudFileWriterService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize FraudFileWriterService: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize FraudFileWriterService", e);
        }
    }

    public void writeTransactionToFile(Transaction transaction, FraudDetectionResult fraudResult) {
        try {
            TransactionRecordDto record = fraudMapper.toTransactionRecord(transaction, fraudResult);

            if (fraudResult.isFraudulent()) {
                writeFraudulentTransaction(record);
                fraudulentTransactionCount.incrementAndGet();
            } else {
                writeNormalTransaction(record);
                normalTransactionCount.incrementAndGet();
            }

            if (enableDetailedReports) {
                writeDetailedReport(transaction, fraudResult);
            }

        } catch (Exception e) {
            log.error("Failed to write transaction {} to file: {}",
                    transaction.getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to write transaction to file", e);
        }
    }

    public FraudStatisticsDto getStatistics() {
        return FraudStatisticsDto.builder()
                .normalTransactionCount(normalTransactionCount.get())
                .fraudulentTransactionCount(fraudulentTransactionCount.get())
                .totalTransactionCount(normalTransactionCount.get() + fraudulentTransactionCount.get())
                .fraudulentPercentage(calculateFraudulentPercentage())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private void createOutputDirectories() throws IOException {
        Path dirPath = Paths.get(outputDirectory);
        createDirectoryIfNotExists(dirPath);

        if (enableDetailedReports) {
            Path detailedReportsPath = dirPath.resolve("detailed-reports");
            createDirectoryIfNotExists(detailedReportsPath);
        }
    }

    private void createDirectoryIfNotExists(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Created directory: {}", dirPath);
        }
    }

    private void initializeTransactionFiles() throws IOException {
        Path dirPath = Paths.get(outputDirectory);

        initializeFileIfNotExists(dirPath.resolve(normalTransactionsFile), "Normal Transactions");
        initializeFileIfNotExists(dirPath.resolve(fraudulentTransactionsFile), "Fraudulent Transactions");
    }

    private void initializeFileIfNotExists(Path filePath, String fileType) throws IOException {
        if (!Files.exists(filePath)) {
            Map<String, Object> initialData = createInitialFileStructure(fileType);
            writeJsonToFile(filePath, initialData);
        }
    }

    private Map<String, Object> createInitialFileStructure(String type) {
        Map<String, Object> data = new HashMap<>();
        data.put("file_type", type);
        data.put("created_at", LocalDateTime.now());
        data.put("last_updated", LocalDateTime.now());
        data.put("total_count", 0);
        data.put("transactions", new HashMap<String, Object>());
        return data;
    }

    private void writeNormalTransaction(TransactionRecordDto record) throws IOException {
        Path filePath = Paths.get(outputDirectory, normalTransactionsFile);
        appendTransactionToFile(filePath, record, "Normal Transactions");
    }

    private void writeFraudulentTransaction(TransactionRecordDto record) throws IOException {
        Path filePath = Paths.get(outputDirectory, fraudulentTransactionsFile);
        appendTransactionToFile(filePath, record, "Fraudulent Transactions");
    }

    @SuppressWarnings("unchecked")
    private void appendTransactionToFile(Path filePath, TransactionRecordDto record, String fileType) throws IOException {
        Map<String, Object> fileData = readOrCreateFileData(filePath, fileType);
        updateFileMetadata(fileData);
        addTransactionToFile(fileData, record);
        writeJsonToFile(filePath, fileData);
    }

    private Map<String, Object> readOrCreateFileData(Path filePath, String fileType) throws IOException {
        if (Files.exists(filePath)) {
            String content = Files.readString(filePath);
            return objectMapper.readValue(content, Map.class);
        } else {
            return createInitialFileStructure(fileType);
        }
    }

    private void updateFileMetadata(Map<String, Object> fileData) {
        fileData.put("last_updated", LocalDateTime.now());
        int currentCount = (Integer) fileData.get("total_count");
        fileData.put("total_count", currentCount + 1);
    }

    @SuppressWarnings("unchecked")
    private void addTransactionToFile(Map<String, Object> fileData, TransactionRecordDto record) {
        Map<String, Object> transactions = (Map<String, Object>) fileData.get("transactions");
        transactions.put(record.getTransactionId(), record);
    }

    private void writeDetailedReport(Transaction transaction, FraudDetectionResult fraudResult) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("fraud_report_%s_%s.json",
                transaction.getTransactionId(), timestamp);

        Path reportPath = Paths.get(outputDirectory, "detailed-reports", filename);

        DetailedFraudReportDto report = fraudMapper.toDetailedReport(transaction, fraudResult);
        writeJsonToFile(reportPath, report);

        log.info("Detailed fraud report written: {}", reportPath);
    }

    private void writeJsonToFile(Path filePath, Object data) throws IOException {
        String jsonContent = objectMapper.writeValueAsString(data);
        Files.writeString(filePath, jsonContent,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private double calculateFraudulentPercentage() {
        long total = normalTransactionCount.get() + fraudulentTransactionCount.get();
        if (total == 0) return 0.0;
        return (double) fraudulentTransactionCount.get() / total * 100.0;
    }
}