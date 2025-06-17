//package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.config.FraudConfigurationService;
//import com.toufik.trxalertservice.model.FraudAlert;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Slf4j
//public abstract class AbstractFraudDetector implements FraudDetector {
//
//    @Autowired
//    protected FraudConfigurationService configurationService;
//
//    @Override
//    public boolean isEnabled() {
//        return configurationService.isDetectorEnabled(getDetectorName());
//    }
//
//    protected FraudAlert createAlert(FraudAlert.FraudType fraudType, String details, int severity) {
//        return FraudAlert.builder()
//                .alertId(UUID.randomUUID().toString())
//                .fraudType(fraudType)
//                .description(fraudType.getDescription())
//                .details(details)
//                .severity(severity)
//                .timestamp(LocalDateTime.now())
//                .status(FraudAlert.AlertStatus.ACTIVE)
//                .build();
//    }
//
//    @Override
//    public int getPriority() {
//        return 1; // Default priority
//    }
//}
//====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Component
//@Slf4j
//public class CrossBorderHighRiskDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "CROSS_BORDER_HIGH_RISK";
//
//    private static final List<String> HIGH_RISK_COUNTRIES = Arrays.asList(
//            "AF", "IR", "KP", "MM", "SY", "YE"
//    );
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        String toCountry = transaction.getToCountryCode();
//        String fromCountry = transaction.getFromCountryCode();
//
//        if (isHighRiskCountry(toCountry) || isHighRiskCountry(fromCountry)) {
//            log.warn("High-risk country detected - Transaction: {}, From: {}, To: {}",
//                    transaction.getTransactionId(), fromCountry, toCountry);
//
//            return createAlert(
//                    FraudAlert.FraudType.CROSS_BORDER_HIGH_RISK,
//                    String.format("Transaction involves high-risk country. From: %s, To: %s",
//                            fromCountry, toCountry),
//                    8 // High severity
//            );
//        }
//
//        return null;
//    }
//
//    private boolean isHighRiskCountry(String countryCode) {
//        return countryCode != null && HIGH_RISK_COUNTRIES.contains(countryCode);
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Component
//@Slf4j
//public class CryptocurrencyKeywordsDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "CRYPTOCURRENCY_KEYWORDS";
//
//    private static final List<String> CRYPTO_KEYWORDS = Arrays.asList(
//            "bitcoin", "ethereum", "cryptocurrency", "crypto", "blockchain",
//            "defi", "nft", "staking", "mining", "wallet", "exchange"
//    );
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        String textToAnalyze = extractTextToAnalyze(transaction);
//
//        if (textToAnalyze != null && containsCryptoKeywords(textToAnalyze)) {
//            log.warn("Cryptocurrency keywords detected - Transaction: {}", transaction.getTransactionId());
//
//            return createAlert(
//                    FraudAlert.FraudType.CRYPTOCURRENCY_KEYWORDS,
//                    String.format("Transaction contains cryptocurrency-related keywords in bank names: %s",
//                            textToAnalyze),
//                    6 // Medium-High severity
//            );
//        }
//
//        return null;
//    }
//
//    private String extractTextToAnalyze(Transaction transaction) {
//        // Use bank names - simple and works with existing data
//        String fromBank = transaction.getFromBankName();
//        String toBank = transaction.getToBankName();
//
//        if (fromBank != null && toBank != null) {
//            return fromBank + " " + toBank;
//        } else if (fromBank != null) {
//            return fromBank;
//        } else if (toBank != null) {
//            return toBank;
//        }
//
//        return null;
//    }
//
//    private boolean containsCryptoKeywords(String text) {
//        if (text == null || text.trim().isEmpty()) {
//            return false;
//        }
//
//        String lowerText = text.toLowerCase();
//        return CRYPTO_KEYWORDS.stream()
//                .anyMatch(lowerText::contains);
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.config.FraudConfigurationService;
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.FraudDetectionResult;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class FraudDetectionService {
//
//    @Autowired
//    private List<FraudDetector> fraudDetectors;
//
//    @Autowired
//    private FraudConfigurationService configurationService;
//
//    public FraudDetectionResult detectFraud(Transaction transaction) {
//        log.info("Starting fraud detection for transaction: {}", transaction.getTransactionId());
//
//        List<FraudAlert> alerts = fraudDetectors.stream()
//                .filter(detector -> detector.isEnabled())
//                .map(detector -> detector.detect(transaction))
//                .filter(alert -> alert != null)
//                .collect(Collectors.toList());
//
//        int totalRiskScore = calculateRiskScore(alerts);
//        FraudDetectionResult.RiskLevel riskLevel = FraudDetectionResult.RiskLevel.fromScore(totalRiskScore);
//
//        boolean isFraudulent = totalRiskScore >= configurationService.getFraudThreshold();
//
//        FraudDetectionResult result = FraudDetectionResult.builder()
//                .transactionId(transaction.getTransactionId())
//                .isFraudulent(isFraudulent)
//                .riskScore(totalRiskScore)
//                .riskLevel(riskLevel)
//                .alerts(alerts)
//                .detectionTimestamp(LocalDateTime.now())
//                .build();
//
//        log.info("Fraud detection completed - Transaction: {}, Risk Score: {}, Fraudulent: {}",
//                transaction.getTransactionId(), totalRiskScore, isFraudulent);
//
//        return result;
//    }
//
//    private int calculateRiskScore(List<FraudAlert> alerts) {
//        return alerts.stream()
//                .mapToInt(FraudAlert::getSeverity)
//                .sum() * 10; // Scale severity to 0-100 range
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//
//public interface FraudDetector {
//    FraudAlert detect(Transaction transaction);
//    boolean isEnabled();
//    String getDetectorName();
//    int getPriority();
//}
//======
//        package com.toufik.trxalertservice.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.toufik.trxalertservice.dto.DetailedFraudReportDto;
//import com.toufik.trxalertservice.dto.FraudStatisticsDto;
//import com.toufik.trxalertservice.dto.TransactionRecordDto;
//import com.toufik.trxalertservice.model.FraudDetectionResult;
//import com.toufik.trxalertservice.model.Transaction;
//import com.toufik.trxalertservice.service.FraudMapper;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicLong;
//
//@Service
//@Slf4j
//public class FraudFileWriterService {
//
//    @Value("${fraud.output.directory:src/main/resources/fraud-detection}")
//    private String outputDirectory;
//
//    @Value("${fraud.output.normal-transactions-file:normal_transactions.json}")
//    private String normalTransactionsFile;
//
//    @Value("${fraud.output.fraudulent-transactions-file:fraudulent_transactions.json}")
//    private String fraudulentTransactionsFile;
//
//    @Value("${fraud.output.detailed-reports:true}")
//    private boolean enableDetailedReports;
//
//    private final ObjectMapper objectMapper;
//    private final FraudMapper fraudMapper;
//    private final AtomicLong normalTransactionCount = new AtomicLong(0);
//    private final AtomicLong fraudulentTransactionCount = new AtomicLong(0);
//
//    public FraudFileWriterService(FraudMapper fraudMapper) {
//        this.fraudMapper = fraudMapper;
//        this.objectMapper = createObjectMapper();
//    }
//
//    private ObjectMapper createObjectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);
//        return mapper;
//    }
//
//    @PostConstruct
//    public void init() {
//        try {
//            createOutputDirectories();
//            initializeTransactionFiles();
//            log.info("FraudFileWriterService initialized successfully");
//        } catch (Exception e) {
//            log.error("Failed to initialize FraudFileWriterService: {}", e.getMessage(), e);
//            throw new RuntimeException("Failed to initialize FraudFileWriterService", e);
//        }
//    }
//
//    public void writeTransactionToFile(Transaction transaction, FraudDetectionResult fraudResult) {
//        try {
//            TransactionRecordDto record = fraudMapper.toTransactionRecord(transaction, fraudResult);
//
//            if (fraudResult.isFraudulent()) {
//                writeFraudulentTransaction(record);
//                fraudulentTransactionCount.incrementAndGet();
//            } else {
//                writeNormalTransaction(record);
//                normalTransactionCount.incrementAndGet();
//            }
//
//            if (enableDetailedReports) {
//                writeDetailedReport(transaction, fraudResult);
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to write transaction {} to file: {}",
//                    transaction.getTransactionId(), e.getMessage(), e);
//            throw new RuntimeException("Failed to write transaction to file", e);
//        }
//    }
//
//    public FraudStatisticsDto getStatistics() {
//        return FraudStatisticsDto.builder()
//                .normalTransactionCount(normalTransactionCount.get())
//                .fraudulentTransactionCount(fraudulentTransactionCount.get())
//                .totalTransactionCount(normalTransactionCount.get() + fraudulentTransactionCount.get())
//                .fraudulentPercentage(calculateFraudulentPercentage())
//                .lastUpdated(LocalDateTime.now())
//                .build();
//    }
//
//    private void createOutputDirectories() throws IOException {
//        Path dirPath = Paths.get(outputDirectory);
//        createDirectoryIfNotExists(dirPath);
//
//        if (enableDetailedReports) {
//            Path detailedReportsPath = dirPath.resolve("detailed-reports");
//            createDirectoryIfNotExists(detailedReportsPath);
//        }
//    }
//
//    private void createDirectoryIfNotExists(Path dirPath) throws IOException {
//        if (!Files.exists(dirPath)) {
//            Files.createDirectories(dirPath);
//            log.info("Created directory: {}", dirPath);
//        }
//    }
//
//    private void initializeTransactionFiles() throws IOException {
//        Path dirPath = Paths.get(outputDirectory);
//
//        initializeFileIfNotExists(dirPath.resolve(normalTransactionsFile), "Normal Transactions");
//        initializeFileIfNotExists(dirPath.resolve(fraudulentTransactionsFile), "Fraudulent Transactions");
//    }
//
//    private void initializeFileIfNotExists(Path filePath, String fileType) throws IOException {
//        if (!Files.exists(filePath)) {
//            Map<String, Object> initialData = createInitialFileStructure(fileType);
//            writeJsonToFile(filePath, initialData);
//        }
//    }
//
//    private Map<String, Object> createInitialFileStructure(String type) {
//        Map<String, Object> data = new HashMap<>();
//        data.put("file_type", type);
//        data.put("created_at", LocalDateTime.now());
//        data.put("last_updated", LocalDateTime.now());
//        data.put("total_count", 0);
//        data.put("transactions", new HashMap<String, Object>());
//        return data;
//    }
//
//    private void writeNormalTransaction(TransactionRecordDto record) throws IOException {
//        Path filePath = Paths.get(outputDirectory, normalTransactionsFile);
//        appendTransactionToFile(filePath, record, "Normal Transactions");
//    }
//
//    private void writeFraudulentTransaction(TransactionRecordDto record) throws IOException {
//        Path filePath = Paths.get(outputDirectory, fraudulentTransactionsFile);
//        appendTransactionToFile(filePath, record, "Fraudulent Transactions");
//    }
//
//    @SuppressWarnings("unchecked")
//    private void appendTransactionToFile(Path filePath, TransactionRecordDto record, String fileType) throws IOException {
//        Map<String, Object> fileData = readOrCreateFileData(filePath, fileType);
//        updateFileMetadata(fileData);
//        addTransactionToFile(fileData, record);
//        writeJsonToFile(filePath, fileData);
//    }
//
//    private Map<String, Object> readOrCreateFileData(Path filePath, String fileType) throws IOException {
//        if (Files.exists(filePath)) {
//            String content = Files.readString(filePath);
//            return objectMapper.readValue(content, Map.class);
//        } else {
//            return createInitialFileStructure(fileType);
//        }
//    }
//
//    private void updateFileMetadata(Map<String, Object> fileData) {
//        fileData.put("last_updated", LocalDateTime.now());
//        int currentCount = (Integer) fileData.get("total_count");
//        fileData.put("total_count", currentCount + 1);
//    }
//
//    @SuppressWarnings("unchecked")
//    private void addTransactionToFile(Map<String, Object> fileData, TransactionRecordDto record) {
//        Map<String, Object> transactions = (Map<String, Object>) fileData.get("transactions");
//        transactions.put(record.getTransactionId(), record);
//    }
//
//    private void writeDetailedReport(Transaction transaction, FraudDetectionResult fraudResult) throws IOException {
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//        String filename = String.format("fraud_report_%s_%s.json",
//                transaction.getTransactionId(), timestamp);
//
//        Path reportPath = Paths.get(outputDirectory, "detailed-reports", filename);
//
//        DetailedFraudReportDto report = fraudMapper.toDetailedReport(transaction, fraudResult);
//        writeJsonToFile(reportPath, report);
//
//        log.info("Detailed fraud report written: {}", reportPath);
//    }
//
//    private void writeJsonToFile(Path filePath, Object data) throws IOException {
//        String jsonContent = objectMapper.writeValueAsString(data);
//        Files.writeString(filePath, jsonContent,
//                StandardOpenOption.CREATE,
//                StandardOpenOption.WRITE,
//                StandardOpenOption.TRUNCATE_EXISTING);
//    }
//
//    private double calculateFraudulentPercentage() {
//        long total = normalTransactionCount.get() + fraudulentTransactionCount.get();
//        if (total == 0) return 0.0;
//        return (double) fraudulentTransactionCount.get() / total * 100.0;
//    }
//}
//====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.dto.DetailedFraudReportDto;
//import com.toufik.trxalertservice.dto.TransactionRecordDto;
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.FraudDetectionResult;
//import com.toufik.trxalertservice.model.Transaction;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//
//@Component
//public class FraudMapper {
//
//    public TransactionRecordDto toTransactionRecord(Transaction transaction, FraudDetectionResult fraudResult) {
//        return TransactionRecordDto.builder()
//                .transactionId(transaction.getTransactionId())
//                .fromAccount(transaction.getFromAccount())
//                .toAccount(transaction.getToAccount())
//                .amount(transaction.getAmount())
//                .currency(transaction.getCurrency())
//                .fromBankName(transaction.getFromBankName())
//                .toBankName(transaction.getToBankName())
//                .fromCountryCode(transaction.getFromCountryCode())
//                .toCountryCode(transaction.getToCountryCode())
//                .timestamp(transaction.getTimestamp())
//                .status(transaction.getStatus())
////                .remittanceInfo(transaction.getRemittanceInfo())
//                .isFraudulent(fraudResult.isFraudulent())
//                .riskScore(fraudResult.getRiskScore())
//                .riskLevel(fraudResult.getRiskLevel())
//                .alertCount(getAlertCount(fraudResult))
//                .alertTypes(getAlertTypes(fraudResult))
//                .detectionTimestamp(fraudResult.getDetectionTimestamp())
//                .processedAt(LocalDateTime.now())
//                .build();
//    }
//
//    public DetailedFraudReportDto toDetailedReport(Transaction transaction, FraudDetectionResult fraudResult) {
//        return DetailedFraudReportDto.builder()
//                .transactionId(transaction.getTransactionId())
//                .transaction(transaction)
//                .fraudDetectionResult(fraudResult)
//                .reportGeneratedAt(LocalDateTime.now())
//                .build();
//    }
//
//    private int getAlertCount(FraudDetectionResult fraudResult) {
//        return fraudResult.getAlerts() != null ? fraudResult.getAlerts().size() : 0;
//    }
//
//    private List<String> getAlertTypes(FraudDetectionResult fraudResult) {
//        if (fraudResult.getAlerts() == null) {
//            return Collections.emptyList();
//        }
//
//        return fraudResult.getAlerts().stream()
//                .map(FraudAlert::getFraudType)
//                .map(Enum::name)
//                .toList();
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//@Component
//@Slf4j
//public class FrequentSmallAmountsDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "FREQUENT_SMALL_AMOUNTS";
//    private static final BigDecimal SMALL_AMOUNT_THRESHOLD = new BigDecimal("1000.00");
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        // Check if transaction amount is small (potential structuring)
//        if (transaction.getAmount().compareTo(SMALL_AMOUNT_THRESHOLD) < 0) {
//            log.warn("Small amount detected for potential structuring - Transaction: {}, Amount: {}",
//                    transaction.getTransactionId(), transaction.getAmount());
//
//            return createAlert(
//                    FraudAlert.FraudType.FREQUENT_SMALL_AMOUNTS,
//                    String.format("Small transaction amount %s may indicate structuring behavior",
//                            transaction.getAmount()),
//                    4 // Medium severity
//            );
//        }
//
//        return null;
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//@Component
//@Slf4j
//public class HighAmountThresholdDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "HIGH_AMOUNT_THRESHOLD";
//    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("15000.00");
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        BigDecimal threshold = configurationService.getHighAmountThreshold().orElse(DEFAULT_THRESHOLD);
//
//        if (transaction.getAmount().compareTo(threshold) >= 0) {
//            log.warn("High amount detected - Transaction: {}, Amount: {}, Threshold: {}",
//                    transaction.getTransactionId(), transaction.getAmount(), threshold);
//
//            return createAlert(
//                    FraudAlert.FraudType.HIGH_AMOUNT_THRESHOLD,
//                    String.format("Transaction amount %s exceeds threshold %s",
//                            transaction.getAmount(), threshold),
//                    7 // High severity
//            );
//        }
//
//        return null;
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Component
//@Slf4j
//public class OffHoursTransactionDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "OFF_HOURS_TRANSACTION";
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        int hour = transaction.getTimestamp().getHour();
//
//        // Off-hours: 2 AM - 5 AM
//        if (hour >= 2 && hour <= 5) {
//            log.warn("Off-hours transaction detected - Transaction: {}, Time: {}",
//                    transaction.getTransactionId(), transaction.getTimestamp());
//
//            return createAlert(
//                    FraudAlert.FraudType.OFF_HOURS_TRANSACTION,
//                    String.format("Transaction occurred during off-hours at %s",
//                            transaction.getTimestamp().toLocalTime()),
//                    5 // Medium severity
//            );
//        }
//
//        return null;
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//@Component
//@Slf4j
//public class RoundAmountPatternDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "ROUND_AMOUNT_PATTERN";
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        if (isRoundAmount(transaction.getAmount())) {
//            log.warn("Round amount pattern detected - Transaction: {}, Amount: {}",
//                    transaction.getTransactionId(), transaction.getAmount());
//
//            return createAlert(
//                    FraudAlert.FraudType.ROUND_AMOUNT_PATTERN,
//                    String.format("Transaction amount %s follows suspicious round pattern",
//                            transaction.getAmount()),
//                    4 // Medium severity
//            );
//        }
//
//        return null;
//    }
//
//    private boolean isRoundAmount(BigDecimal amount) {
//        double amountValue = amount.doubleValue();
//        return amountValue % 1000 == 0 || amountValue % 5000 == 0 || amountValue % 2000 == 0;
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//@Component
//@Slf4j
//public class StructuringPatternDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "STRUCTURING_PATTERN";
//    private static final BigDecimal STRUCTURING_THRESHOLD = new BigDecimal("10000.00");
//    private static final BigDecimal STRUCTURING_MARGIN = new BigDecimal("100.00");
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        BigDecimal amount = transaction.getAmount();
//        BigDecimal lowerBound = STRUCTURING_THRESHOLD.subtract(STRUCTURING_MARGIN);
//
//        // Check if amount is just under the reporting threshold
//        if (amount.compareTo(lowerBound) >= 0 && amount.compareTo(STRUCTURING_THRESHOLD) < 0) {
//            log.warn("Structuring pattern detected - Transaction: {}, Amount: {}",
//                    transaction.getTransactionId(), amount);
//
//            return createAlert(
//                    FraudAlert.FraudType.STRUCTURING_PATTERN,
//                    String.format("Transaction amount %s appears to be structured to avoid reporting threshold",
//                            amount),
//                    7 // High severity
//            );
//        }
//
//        return null;
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudAlert;
//import com.toufik.trxalertservice.model.Transaction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Component
//@Slf4j
//public class SuspiciousRemittanceDetector extends AbstractFraudDetector {
//
//    private static final String DETECTOR_NAME = "SUSPICIOUS_REMITTANCE";
//
//    private static final List<String> SUSPICIOUS_KEYWORDS = Arrays.asList(
//            "yieldfarm", "guaranteed returns", "crypto investment", "urgent cash",
//            "confidential", "mining pool", "trading profit", "forex trading",
//            "binary options", "investment opportunity"
//    );
//
//    @Override
//    public FraudAlert detect(Transaction transaction) {
//        String textToAnalyze = extractTextToAnalyze(transaction);
//
//        if (textToAnalyze != null && containsSuspiciousKeywords(textToAnalyze)) {
//            log.warn("Suspicious remittance detected - Transaction: {}", transaction.getTransactionId());
//
//            return createAlert(
//                    FraudAlert.FraudType.SUSPICIOUS_REMITTANCE,
//                    String.format("Bank information contains suspicious keywords: %s",
//                            textToAnalyze),
//                    6 // Medium-High severity
//            );
//        }
//
//        return null;
//    }
//
//    private String extractTextToAnalyze(Transaction transaction) {
//        // Use bank names - simple and works with existing data
//        String fromBank = transaction.getFromBankName();
//        String toBank = transaction.getToBankName();
//
//        if (fromBank != null && toBank != null) {
//            return fromBank + " " + toBank;
//        } else if (fromBank != null) {
//            return fromBank;
//        } else if (toBank != null) {
//            return toBank;
//        }
//
//        return null;
//    }
//
//    private boolean containsSuspiciousKeywords(String text) {
//        if (text == null || text.trim().isEmpty()) {
//            return false;
//        }
//
//        String lowerText = text.toLowerCase();
//        return SUSPICIOUS_KEYWORDS.stream()
//                .anyMatch(lowerText::contains);
//    }
//
//    @Override
//    public String getDetectorName() {
//        return DETECTOR_NAME;
//    }
//}
//======
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.Transaction;
//import com.toufik.trxalertservice.model.TransactionWithMT103Event;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Stream;
//
//@Service
//@Slf4j
//public class SwiftFileReaderService {
//
//    @Value("${swift.output.directory:src/main/resources/swift-files}")
//    private String outputDirectory;
//
//    @Value("${swift.output.file-extension:.swift}")
//    private String fileExtension;
//
//    private static List<String> TRX_STATUS = List.of(
//            "RECEIVED",
//            "VALIDATED",
//            "FRAUD_SUSPECTED",
//            "FRAUD_CONFIRMED",
//            "FRAUD_CLEARED",
//            "PROCESSED",
//            "REJECTED",
//            "ERROR",
//            "RETRYING",
//            "QUARANTINED",
//            "FORWARDED"
//    );
//
//    public List<TransactionWithMT103Event> readAllSwiftFiles() {
//        List<TransactionWithMT103Event> transactions = new ArrayList<>();
//
//        try {
//            Path dirPath = Paths.get(outputDirectory);
//
//            if (!Files.exists(dirPath)) {
//                log.warn("SWIFT files directory does not exist: {}", outputDirectory);
//                return transactions;
//            }
//
//            try (Stream<Path> paths = Files.walk(dirPath)) {
//                paths.filter(Files::isRegularFile)
//                        .filter(path -> path.toString().endsWith(fileExtension))
//                        .forEach(path -> {
//                            try {
//                                TransactionWithMT103Event transaction = parseSwiftFile(path);
//                                if (transaction != null) {
//                                    transactions.add(transaction);
//                                }
//                            } catch (Exception e) {
//                                log.error("Error parsing SWIFT file {}: {}", path.getFileName(), e.getMessage());
//                            }
//                        });
//            }
//
//            log.info("Successfully read {} SWIFT files from {}", transactions.size(), outputDirectory);
//
//        } catch (IOException e) {
//            log.error("Error reading SWIFT files from directory {}: {}", outputDirectory, e.getMessage());
//            throw new RuntimeException("Failed to read SWIFT files", e);
//        }
//
//        return transactions;
//    }
//
//    private TransactionWithMT103Event parseSwiftFile(Path filePath) throws IOException {
//        String content = Files.readString(filePath);
//        String[] lines = content.split("\n");
//
//        Transaction transaction = new Transaction();
//        StringBuilder mt103Content = new StringBuilder();
//        boolean inMT103Section = false;
//
//        for (String line : lines) {
//            line = line.trim();
//
//            // Skip empty lines
//            if (line.isEmpty()) {
//                continue;
//            }
//
//            // Parse header comments for transaction metadata
//            if (line.startsWith("// Transaction ID:")) {
//                transaction.setTransactionId(extractValue(line, "// Transaction ID:"));
//            } else if (line.startsWith("// Amount:")) {
//                parseAmount(line, transaction);
//            } else if (line.startsWith("// From:")) {
//                transaction.setFromBankName(extractValue(line, "// From:"));
//            } else if (line.startsWith("// To:")) {
//                transaction.setToBankName(extractValue(line, "// To:"));
//            } else if (line.startsWith("// Generated at:")) {
//                parseTimestamp(line, transaction);
//            } else if (line.equals("//=====================================")) {
//                inMT103Section = true;
//                continue;
//            }
//
//            // Collect MT103 content (everything after the separator that's not a comment)
//            if (inMT103Section && !line.startsWith("//")) {
//                if (mt103Content.length() > 0) {
//                    mt103Content.append("\n");
//                }
//                mt103Content.append(line);
//            }
//        }
//
//        // Extract additional information from MT103 content if available
//        String mt103String = mt103Content.toString();
//        if (!mt103String.isEmpty() && !mt103String.contains("No MT103 content available")) {
//            parseAdditionalFieldsFromMT103(mt103String, transaction);
//        }
//
//        // Set default values if not found
//        setDefaultValues(transaction, filePath.getFileName().toString());
//
//        TransactionWithMT103Event event = new TransactionWithMT103Event();
//        event.setTransaction(transaction);
//        event.setMt103Content(mt103String.isEmpty() ? null : mt103String);
//
//        return event;
//    }
//
//    private String extractValue(String line, String prefix) {
//        return line.substring(prefix.length()).trim();
//    }
//
//    private void parseAmount(String line, Transaction transaction) {
//        String amountStr = extractValue(line, "// Amount:");
//        String[] parts = amountStr.split(" ");
//        if (parts.length >= 2) {
//            try {
//                transaction.setAmount(new BigDecimal(parts[0]));
//                transaction.setCurrency(parts[1]);
//            } catch (NumberFormatException e) {
//                log.warn("Failed to parse amount: {}", amountStr);
//            }
//        }
//    }
//
//    private void parseTimestamp(String line, Transaction transaction) {
//        String timestampStr = extractValue(line, "// Generated at:");
//        try {
//            // Try to parse the timestamp (adjust format as needed)
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
//            transaction.setTimestamp(LocalDateTime.parse(timestampStr, formatter));
//        } catch (DateTimeParseException e) {
//            try {
//                // Try alternative format
//                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//                transaction.setTimestamp(LocalDateTime.parse(timestampStr, altFormatter));
//            } catch (DateTimeParseException e2) {
//                log.warn("Failed to parse timestamp: {}", timestampStr);
//                transaction.setTimestamp(LocalDateTime.now());
//            }
//        }
//    }
//
//    private void parseAdditionalFieldsFromMT103(String mt103Content, Transaction transaction) {
//        // Parse MT103 fields to extract additional transaction information
//        // This is a simplified parser - you might need to adjust based on your MT103 format
//
//        // Example MT103 field patterns
//        parseFieldFromMT103(mt103Content, ":20:", transaction::setTransactionId);
//        parseFieldFromMT103(mt103Content, ":32A:", (value) -> {
//            // Format: YYMMDDCCCAMOUNT
//            if (value.length() > 9) {
//                String currency = value.substring(6, 9);
//                String amount = value.substring(9);
//                transaction.setCurrency(currency);
//                try {
//                    // Remove any non-numeric characters except decimal point
//                    String cleanAmount = amount.replaceAll("[^0-9.]", "");
//                    transaction.setAmount(new BigDecimal(cleanAmount));
//                } catch (NumberFormatException e) {
//                    log.warn("Failed to parse amount from MT103: {}", amount);
//                }
//            }
//        });
//
//        parseFieldFromMT103(mt103Content, ":50K:", (value) -> {
//            // Ordering customer - extract account info
//            String[] lines = value.split("\n");
//            if (lines.length > 0) {
//                transaction.setFromAccount(lines[0].trim());
//            }
//        });
//
//        parseFieldFromMT103(mt103Content, ":59:", (value) -> {
//            // Beneficiary customer - extract account info
//            String[] lines = value.split("\n");
//            if (lines.length > 0) {
//                transaction.setToAccount(lines[0].trim());
//            }
//        });
//
//        parseFieldFromMT103(mt103Content, ":52A:", (value) -> {
//            // Ordering institution
//            if (value.length() > 8) {
//                transaction.setFromBankSwift(value.substring(0, 8));
//            }
//        });
//
//        parseFieldFromMT103(mt103Content, ":57A:", (value) -> {
//            // Account with institution
//            if (value.length() > 8) {
//                transaction.setToBankSwift(value.substring(0, 8));
//            }
//        });
//    }
//
//    private void parseFieldFromMT103(String mt103Content, String fieldCode, java.util.function.Consumer<String> setter) {
//        Pattern pattern = Pattern.compile(fieldCode + "([^\n:]+(?:\n[^:][^\n]*)*)");
//        Matcher matcher = pattern.matcher(mt103Content);
//        if (matcher.find()) {
//            String value = matcher.group(1).trim();
//            setter.accept(value);
//        }
//    }
//
//    private void setDefaultValues(Transaction transaction, String filename) {
//        if (transaction.getTransactionId() == null) {
//            // Extract transaction ID from filename if not found in content
//            Pattern pattern = Pattern.compile("MT103_([^_]+)_");
//            Matcher matcher = pattern.matcher(filename);
//            if (matcher.find()) {
//                transaction.setTransactionId(matcher.group(1));
//            } else {
//                transaction.setTransactionId("UNKNOWN_" + System.currentTimeMillis());
//            }
//        }
//
//        if (transaction.getTimestamp() == null) {
//            transaction.setTimestamp(LocalDateTime.now());
//        }
//
//        if (transaction.getStatus() == null) {
//            transaction.setStatus(TRX_STATUS.get(new Random().nextInt(TRX_STATUS.size())));
//        }
//
//        if (transaction.getAmount() == null) {
//            transaction.setAmount(BigDecimal.ZERO);
//        }
//
//        if (transaction.getCurrency() == null) {
//            transaction.setCurrency("USD");
//        }
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudDetectionResult;
//import com.toufik.trxalertservice.model.TransactionWithMT103Event;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//@Service
//@Slf4j
//public class SwiftFileWriterService {
//
//    @Value("${swift.output.directory:src/main/resources/swift-files}")
//    private String outputDirectory;
//
//    @Value("${swift.output.file-extension:.swift}")
//    private String fileExtension;
//
//    @PostConstruct
//    public void init() {
//        try {
//            createOutputDirectories();
//            log.info("SwiftFileWriterService initialized successfully");
//        } catch (Exception e) {
//            log.error("Failed to initialize SwiftFileWriterService: {}", e.getMessage(), e);
//            throw new RuntimeException("Failed to initialize SwiftFileWriterService", e);
//        }
//    }
//
//    public void writeSwiftFile(TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
//        try {
//            Path filePath = createFilePath(event, fraudResult);
//            String content = generateFileContent(event, fraudResult);
//
//            writeContentToFile(filePath, content);
//
//            log.info("Successfully wrote SWIFT file: {} ({})",
//                    filePath.toString(), fraudResult.isFraudulent() ? "FRAUDULENT" : "NORMAL");
//
//        } catch (IOException e) {
//            log.error("Failed to write SWIFT file for transaction {}: {}",
//                    event.getTransaction().getTransactionId(), e.getMessage(), e);
//            throw new RuntimeException("Failed to write SWIFT file", e);
//        }
//    }
//
//    private void createOutputDirectories() throws IOException {
//        Path dirPath = Paths.get(outputDirectory);
//        createDirectoryIfNotExists(dirPath);
//
//        // Create subdirectories for normal and fraudulent transactions
//        createDirectoryIfNotExists(dirPath.resolve("normal"));
//        createDirectoryIfNotExists(dirPath.resolve("fraudulent"));
//    }
//
//    private void createDirectoryIfNotExists(Path dirPath) throws IOException {
//        if (!Files.exists(dirPath)) {
//            Files.createDirectories(dirPath);
//            log.info("Created directory: {}", dirPath);
//        }
//    }
//
//    private Path createFilePath(TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
//        String subDirectory = fraudResult.isFraudulent() ? "fraudulent" : "normal";
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//        String filename = String.format("MT103_%s_%s%s",
//                event.getTransaction().getTransactionId(),
//                timestamp,
//                fileExtension);
//
//        return Paths.get(outputDirectory, subDirectory, filename);
//    }
//
//    private String generateFileContent(TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
//        StringBuilder content = new StringBuilder();
//
//        appendHeader(content, event, fraudResult);
//        appendMT103Content(content, event);
//
//        return content.toString();
//    }
//
//    private void appendHeader(StringBuilder content, TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
//        content.append("// SWIFT MT103 Message\n");
//        content.append("// Generated at: ").append(LocalDateTime.now()).append("\n");
//        content.append("// Transaction ID: ").append(event.getTransaction().getTransactionId()).append("\n");
//        content.append("// Amount: ").append(event.getTransaction().getAmount())
//                .append(" ").append(event.getTransaction().getCurrency()).append("\n");
//        content.append("// From: ").append(event.getTransaction().getFromBankName()).append("\n");
//        content.append("// To: ").append(event.getTransaction().getToBankName()).append("\n");
//        content.append("// FRAUD STATUS: ").append(fraudResult.isFraudulent() ? "FRAUDULENT" : "NORMAL").append("\n");
//        content.append("// Risk Score: ").append(fraudResult.getRiskScore()).append("/100\n");
//        content.append("// Risk Level: ").append(fraudResult.getRiskLevel()).append("\n");
//
//        if (fraudResult.getAlerts() != null && !fraudResult.getAlerts().isEmpty()) {
//            content.append("// Fraud Alerts: ").append(fraudResult.getAlerts().size()).append("\n");
//            fraudResult.getAlerts().forEach(alert ->
//                    content.append("//   - ").append(alert.getFraudType().getDescription()).append("\n"));
//        }
//
//        content.append("//=====================================\n\n");
//    }
//
//    private void appendMT103Content(StringBuilder content, TransactionWithMT103Event event) {
//        if (event.getMt103Content() != null && !event.getMt103Content().trim().isEmpty()) {
//            content.append(event.getMt103Content());
//        } else {
//            content.append("// No MT103 content available");
//        }
//    }
//
//    private void writeContentToFile(Path filePath, String content) throws IOException {
//        Files.write(filePath, content.getBytes(),
//                StandardOpenOption.CREATE,
//                StandardOpenOption.WRITE,
//                StandardOpenOption.TRUNCATE_EXISTING);
//    }
//}
//=====
//        package com.toufik.trxalertservice.service;
//
//import com.toufik.trxalertservice.model.FraudDetectionResult;
//import com.toufik.trxalertservice.model.TransactionWithMT103Event;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Service;
//
//@Service
//@Slf4j
//public class TransactionAlertConsumerService {
//
//    private final FraudDetectionService fraudDetectionService;
//    private final FraudFileWriterService fraudFileWriterService;
//    private final SwiftFileWriterService swiftFileWriterService;
//
//    @Autowired
//    public TransactionAlertConsumerService(FraudDetectionService fraudDetectionService,
//                                           FraudFileWriterService fraudFileWriterService,
//                                           SwiftFileWriterService swiftFileWriterService) {
//        this.fraudDetectionService = fraudDetectionService;
//        this.fraudFileWriterService = fraudFileWriterService;
//        this.swiftFileWriterService = swiftFileWriterService;
//    }
//
//    @KafkaListener(
//            topics = "transactions_alert",
//            groupId = "transaction-alert-group"
//    )
//    public void consumeTransactionAlert(
//            @Payload TransactionWithMT103Event transactionWithMT103Event,
//            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
//
//        String transactionId = transactionWithMT103Event.getTransaction().getTransactionId();
//
//        try {
//            logTransactionDetails(transactionWithMT103Event, topic);
//
//            // Perform fraud detection
//            FraudDetectionResult fraudResult = fraudDetectionService.detectFraud(
//                    transactionWithMT103Event.getTransaction());
//
//            // Write transaction to appropriate fraud detection file
//            fraudFileWriterService.writeTransactionToFile(
//                    transactionWithMT103Event.getTransaction(), fraudResult);
//
//            // Write MT103 content to SWIFT file
//            swiftFileWriterService.writeSwiftFile(transactionWithMT103Event, fraudResult);
//
//            logFraudDetectionResult(fraudResult);
//
//            log.debug("Transaction alert {} processed successfully", transactionId);
//
//        } catch (Exception e) {
//            log.error("Error processing transaction alert {}: {}", transactionId, e.getMessage(), e);
//            // Could add dead letter queue or retry logic here
//        }
//    }
//
//    private void logTransactionDetails(TransactionWithMT103Event event, String topic) {
//        log.info("======================= ALERT SERVICE RECEIVED TRANSACTION =============================");
//        log.info("Kafka Message Details:");
//        log.info("  Topic: {}", topic);
//
//        log.info("Transaction Details:");
//        log.info("  Transaction ID: {}", event.getTransaction().getTransactionId());
//        log.info("  From Account: {}", event.getTransaction().getFromAccount());
//        log.info("  To Account: {}", event.getTransaction().getToAccount());
//        log.info("  Amount: {} {}",
//                event.getTransaction().getAmount(),
//                event.getTransaction().getCurrency());
//        log.info("  From Bank: {}", event.getTransaction().getFromBankName());
//        log.info("  To Bank: {}", event.getTransaction().getToBankName());
//        log.info("  Status: {}", event.getTransaction().getStatus());
//    }
//
//    private void logFraudDetectionResult(FraudDetectionResult fraudResult) {
//        log.info("Fraud Detection Result:");
//        log.info("  Is Fraudulent: {}", fraudResult.isFraudulent());
//        log.info("  Risk Score: {}", fraudResult.getRiskScore());
//        log.info("  Risk Level: {}", fraudResult.getRiskLevel());
//        log.info("  Alert Count: {}", fraudResult.getAlerts() != null ? fraudResult.getAlerts().size() : 0);
//        log.info("==================================================================");
//    }
//}
//=======
