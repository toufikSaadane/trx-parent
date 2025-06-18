package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class TransactionConsumerService {

    @Autowired
    private TransactionProducerService producerService;

    private final AtomicLong consumedCount = new AtomicLong(0);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @KafkaListener(topics = "transaction_generator",
            groupId = "transaction-validator-group",
            properties = {"auto.offset.reset=earliest"}
    )
    public void consume(
            @Payload TransactionWithMT103Event event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        long currentCount = consumedCount.incrementAndGet();
        String transactionId = event.getTransaction().getTransactionId();

        try {
            log.info("════════════════ CONSUMED TRANSACTION #{} ════════════════", currentCount);
            log.info("📨 Kafka Info: Topic={}, Partition={}, Offset={}", topic, partition, offset);
            log.info("⏰ Processing Time: {}", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

            logTransactionDetails(event);
            logMT103Analysis(event.getMt103Content());

            // Forward to producer for alert processing
            producerService.sendTransactionAlert(event);

            log.info("✅ Transaction {} successfully processed and forwarded", transactionId);

        } catch (Exception e) {
            log.error("❌ Error consuming transaction {}: {}", transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to process transaction: " + transactionId, e);
        } finally {
            log.info("═══════════════════════════════════════════════════════════════");
        }
    }

    private void logTransactionDetails(TransactionWithMT103Event event) {
        var trx = event.getTransaction();

        log.info("💳 Transaction Details:");
        log.info("   ID: {}", trx.getTransactionId());
        log.info("   From: {} [{}]", trx.getFromAccount(), trx.getFromBankName());
        log.info("   To: {} [{}]", trx.getToAccount(), trx.getToBankName());
        log.info("   Amount: {} {}", trx.getAmount(), trx.getCurrency());
        log.info("   Status: {}", trx.getStatus());
        log.info("   Timestamp: {}", trx.getTimestamp());

        // Calculate transaction metrics
        if (trx.getAmount() != null) {
            try {
                double amount = Double.parseDouble(trx.getAmount().toString());
                if (amount > 10000) {
                    log.warn("🚨 HIGH VALUE TRANSACTION: {} {}", amount, trx.getCurrency());
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ Could not parse amount for analysis: {}", trx.getAmount());
            }
        }
    }

    private void logMT103Analysis(String content) {
        if (content == null) {
            log.warn("⚠️ MT103 content is null");
            return;
        }

        log.info("📄 MT103 Message Analysis:");
        log.info("   Size: {} characters", content.length());

        // Analyze MT103 structure
        analyzeMT103Structure(content);

        // Show preview of content
        logMT103Preview(content);

        // Perform basic validation checks
        performBasicValidation(content);
    }

    private void analyzeMT103Structure(String content) {
        int headerBlocks = 0;
        if (content.contains("{1:")) headerBlocks++;
        if (content.contains("{2:")) headerBlocks++;
        if (content.contains("{3:")) headerBlocks++;
        if (content.contains("{4:")) headerBlocks++;
        if (content.contains("{5:")) headerBlocks++;

        log.info("   Structure: {} header blocks detected", headerBlocks);

        // Count mandatory fields
        int mandatoryFields = 0;
        if (content.contains(":20:")) mandatoryFields++;
        if (content.contains(":23B:")) mandatoryFields++;
        if (content.contains(":32A:")) mandatoryFields++;

        log.info("   Mandatory Fields: {}/3 present", mandatoryFields);

        // Check for optional fields
        String[] optionalFields = {":50K:", ":52A:", ":57A:", ":59:", ":70:", ":71A:"};
        int optionalCount = 0;
        for (String field : optionalFields) {
            if (content.contains(field)) {
                optionalCount++;
            }
        }
        log.info("   Optional Fields: {} detected", optionalCount);
    }

    private void logMT103Preview(String content) {
        log.info("   Preview (first 10 lines):");
        String[] lines = content.split("\\n");
        int linesToShow = Math.min(10, lines.length);

        for (int i = 0; i < linesToShow; i++) {
            String line = lines[i].trim();
            if (line.length() > 80) {
                line = line.substring(0, 77) + "...";
            }
            log.info("     {}: {}", String.format("%2d", i + 1), line);
        }

        if (lines.length > 10) {
            log.info("     ... ({} more lines)", lines.length - 10);
        }
    }

    private void performBasicValidation(String content) {
        log.info("   Quick Validation:");

        // Check basic structure
        boolean hasValidStart = content.trim().startsWith("{");
        boolean hasValidEnd = content.trim().endsWith("}");
        boolean hasDoubleColons = content.contains("::");
        boolean hasControlChars = hasControlCharacters(content);

        log.info("     ✓ Proper start/end: {}/{}", hasValidStart, hasValidEnd);
        log.info("     ✓ No double colons: {}", !hasDoubleColons);
        log.info("     ✓ No control chars: {}", !hasControlChars);

        // Estimate message health
        int healthScore = 0;
        if (hasValidStart) healthScore += 25;
        if (hasValidEnd) healthScore += 25;
        if (!hasDoubleColons) healthScore += 25;
        if (!hasControlChars) healthScore += 25;

        String healthStatus = healthScore >= 75 ? "GOOD" :
                healthScore >= 50 ? "FAIR" : "POOR";
        log.info("     📊 Health Score: {}/100 ({})", healthScore, healthStatus);
    }

    private boolean hasControlCharacters(String content) {
        for (char c : content.toCharArray()) {
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        return false;
    }
}