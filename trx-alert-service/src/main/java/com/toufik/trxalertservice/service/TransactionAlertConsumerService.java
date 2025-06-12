package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
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
public class TransactionAlertConsumerService {

    @Value("${swift.output.directory:src/main/resources/swift-files}")
    private String outputDirectory;

    @Value("${swift.output.file-extension:.swift}")
    private String fileExtension;

    @KafkaListener(
            topics = "transactions_alert",
            groupId = "transaction-alert-group"
    )
    public void consumeTransactionAlert(
            @Payload TransactionWithMT103Event transactionWithMT103Event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
            ){
        try {
            log.info("======================= ALERT SERVICE RECEIVED TRANSACTION =============================");
            log.info("Kafka Message Details:");
            log.info("  Topic: {}", topic);

            log.info("Transaction Details:");
            log.info("  Transaction ID: {}", transactionWithMT103Event.getTransaction().getTransactionId());
            log.info("  From Account: {}", transactionWithMT103Event.getTransaction().getFromAccount());
            log.info("  To Account: {}", transactionWithMT103Event.getTransaction().getToAccount());
            log.info("  Amount: {} {}",
                    transactionWithMT103Event.getTransaction().getAmount(),
                    transactionWithMT103Event.getTransaction().getCurrency());
            log.info("  From Bank: {}", transactionWithMT103Event.getTransaction().getFromBankName());
            log.info("  To Bank: {}", transactionWithMT103Event.getTransaction().getToBankName());
            log.info("  Status: {}", transactionWithMT103Event.getTransaction().getStatus());

            // Write MT103 content to SWIFT file
            writeSwiftFile(transactionWithMT103Event);

            log.info("==================================================================");

            // Acknowledge successful processing
            log.debug("Transaction alert {} processed successfully",
                    transactionWithMT103Event.getTransaction().getTransactionId());

        } catch (Exception e) {
            log.error("Error processing transaction alert {}: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    e.getMessage(), e);

            // Acknowledge to prevent infinite reprocessing
        }
    }

    private void writeSwiftFile(TransactionWithMT103Event event) {
        try {
            // Create output directory if it doesn't exist
            Path dirPath = Paths.get(outputDirectory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("Created output directory: {}", outputDirectory);
            }

            // Generate filename with transaction ID and timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("MT103_%s_%s%s",
                    event.getTransaction().getTransactionId(),
                    timestamp,
                    fileExtension);

            Path filePath = dirPath.resolve(filename);

            // Prepare file content
            StringBuilder content = new StringBuilder();
            content.append("// SWIFT MT103 Message\n");
            content.append("// Generated at: ").append(LocalDateTime.now()).append("\n");
            content.append("// Transaction ID: ").append(event.getTransaction().getTransactionId()).append("\n");
            content.append("// Amount: ").append(event.getTransaction().getAmount())
                    .append(" ").append(event.getTransaction().getCurrency()).append("\n");
            content.append("// From: ").append(event.getTransaction().getFromBankName()).append("\n");
            content.append("// To: ").append(event.getTransaction().getToBankName()).append("\n");
            content.append("//=====================================\n\n");

            if (event.getMt103Content() != null) {
                content.append(event.getMt103Content());
            } else {
                content.append("// No MT103 content available");
            }

            // Write to file
            Files.write(filePath, content.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Successfully wrote SWIFT file: {}", filePath.toString());

        } catch (IOException e) {
            log.error("Failed to write SWIFT file for transaction {}: {}",
                    event.getTransaction().getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to write SWIFT file", e);
        }
    }
}