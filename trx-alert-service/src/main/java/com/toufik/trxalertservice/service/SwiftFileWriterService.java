package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudDetectionResult;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
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

@Service
@Slf4j
public class SwiftFileWriterService {

    @Value("${swift.output.directory:src/main/resources/swift-files}")
    private String outputDirectory;

    @Value("${swift.output.file-extension:.swift}")
    private String fileExtension;

    @PostConstruct
    public void init() {
        try {
            createOutputDirectories();
            log.info("SwiftFileWriterService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize SwiftFileWriterService: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize SwiftFileWriterService", e);
        }
    }

    public void writeSwiftFile(TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
        try {
            Path filePath = createFilePath(event, fraudResult);
            String content = generateFileContent(event, fraudResult);

            writeContentToFile(filePath, content);

            log.info("Successfully wrote SWIFT file: {} ({})",
                    filePath.toString(), fraudResult.isFraudulent() ? "FRAUDULENT" : "NORMAL");

        } catch (IOException e) {
            log.error("Failed to write SWIFT file for transaction {}: {}",
                    event.getTransaction().getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to write SWIFT file", e);
        }
    }

    private void createOutputDirectories() throws IOException {
        Path dirPath = Paths.get(outputDirectory);
        createDirectoryIfNotExists(dirPath);

        // Create subdirectories for normal and fraudulent transactions
        createDirectoryIfNotExists(dirPath.resolve("normal"));
        createDirectoryIfNotExists(dirPath.resolve("fraudulent"));
    }

    private void createDirectoryIfNotExists(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Created directory: {}", dirPath);
        }
    }

    private Path createFilePath(TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
        String subDirectory = fraudResult.isFraudulent() ? "fraudulent" : "normal";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("MT103_%s_%s%s",
                event.getTransaction().getTransactionId(),
                timestamp,
                fileExtension);

        return Paths.get(outputDirectory, subDirectory, filename);
    }

    private String generateFileContent(TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
        StringBuilder content = new StringBuilder();

        appendHeader(content, event, fraudResult);
        appendMT103Content(content, event);

        return content.toString();
    }

    private void appendHeader(StringBuilder content, TransactionWithMT103Event event, FraudDetectionResult fraudResult) {
        content.append("// SWIFT MT103 Message\n");
        content.append("// Generated at: ").append(LocalDateTime.now()).append("\n");
        content.append("// Transaction ID: ").append(event.getTransaction().getTransactionId()).append("\n");
        content.append("// Amount: ").append(event.getTransaction().getAmount())
                .append(" ").append(event.getTransaction().getCurrency()).append("\n");
        content.append("// From: ").append(event.getTransaction().getFromBankName()).append("\n");
        content.append("// To: ").append(event.getTransaction().getToBankName()).append("\n");
        content.append("// FRAUD STATUS: ").append(fraudResult.isFraudulent() ? "FRAUDULENT" : "NORMAL").append("\n");
        content.append("// Risk Score: ").append(fraudResult.getRiskScore()).append("/100\n");
        content.append("// Risk Level: ").append(fraudResult.getRiskLevel()).append("\n");

        if (fraudResult.getAlerts() != null && !fraudResult.getAlerts().isEmpty()) {
            content.append("// Fraud Alerts: ").append(fraudResult.getAlerts().size()).append("\n");
            fraudResult.getAlerts().forEach(alert ->
                    content.append("//   - ").append(alert.getFraudType().getDescription()).append("\n"));
        }

        content.append("//=====================================\n\n");
    }

    private void appendMT103Content(StringBuilder content, TransactionWithMT103Event event) {
        if (event.getMt103Content() != null && !event.getMt103Content().trim().isEmpty()) {
            content.append(event.getMt103Content());
        } else {
            content.append("// No MT103 content available");
        }
    }

    private void writeContentToFile(Path filePath, String content) throws IOException {
        Files.write(filePath, content.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
}