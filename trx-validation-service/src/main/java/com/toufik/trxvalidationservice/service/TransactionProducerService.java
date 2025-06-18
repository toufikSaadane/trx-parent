package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class TransactionProducerService {

    private static final String TOPIC = "transaction_alert";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private KafkaTemplate<String, TransactionWithMT103Event> kafkaTemplate;

    private final AtomicLong sentCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    public void sendTransactionAlert(TransactionWithMT103Event event) {
        String transactionId = event.getTransaction().getTransactionId();
        long currentSent = sentCount.incrementAndGet();

        log.info("🚀 Sending transaction alert #{} for ID: {}", currentSent, transactionId);
        log.info("   📤 Target Topic: {}", TOPIC);
        log.info("   ⏰ Send Time: {}", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        try {
            // Validate event before sending
            validateEvent(event);

            CompletableFuture<SendResult<String, TransactionWithMT103Event>> future =
                    kafkaTemplate.send(TOPIC, transactionId, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSendSuccess(result, transactionId);
                } else {
                    handleSendFailure(ex, transactionId);
                }
            });

        } catch (Exception e) {
            log.error("❌ Exception during send operation for transaction {}: {}",
                    transactionId, e.getMessage(), e);
            failureCount.incrementAndGet();
            throw new RuntimeException("Kafka send failure for transaction: " + transactionId, e);
        }
    }

    private void validateEvent(TransactionWithMT103Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getTransaction() == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (event.getTransaction().getTransactionId() == null ||
                event.getTransaction().getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }

        if (event.getMt103Content() == null || event.getMt103Content().trim().isEmpty()) {
            log.warn("⚠️ MT103 content is null or empty for transaction: {}",
                    event.getTransaction().getTransactionId());
        }
    }

    private void handleSendSuccess(SendResult<String, TransactionWithMT103Event> result,
                                   String transactionId) {
        long currentSuccess = successCount.incrementAndGet();
        var metadata = result.getRecordMetadata();

        log.info("✅ Transaction {} sent successfully", transactionId);
        log.info("   📊 Success #{} - Partition: {}, Offset: {}",
                currentSuccess, metadata.partition(), metadata.offset());
        log.info("   📏 Message size: {} bytes", metadata.serializedValueSize());
        log.info("   ⏱️ Timestamp: {}",
                LocalDateTime.ofEpochSecond(metadata.timestamp() / 1000, 0,
                        java.time.ZoneOffset.UTC).format(TIMESTAMP_FORMATTER));

        logStatistics();
    }

    private void handleSendFailure(Throwable ex, String transactionId) {
        long currentFailure = failureCount.incrementAndGet();

        log.error("❌ Failed to send transaction {} (Failure #{})", transactionId, currentFailure);
        log.error("   💥 Error: {}", ex.getMessage());
        log.error("   🔍 Error Type: {}", ex.getClass().getSimpleName());

        // Log additional details for specific exception types
        if (ex instanceof org.apache.kafka.common.errors.TimeoutException) {
            log.error("   ⏱️ Timeout occurred - check broker connectivity");
        } else if (ex instanceof org.apache.kafka.common.errors.NotLeaderForPartitionException) {
            log.error("   🔄 Leadership change detected - retry may succeed");
        } else if (ex instanceof org.apache.kafka.common.errors.RecordTooLargeException) {
            log.error("   📏 Message too large - check message size limits");
        }

        logStatistics();
    }

    private void logStatistics() {
        long sent = sentCount.get();
        long success = successCount.get();
        long failure = failureCount.get();

        if (sent % 10 == 0) { // Log stats every 10 sends
            double successRate = sent > 0 ? (double) success / sent * 100 : 0.0;
            double failureRate = sent > 0 ? (double) failure / sent * 100 : 0.0;

            log.info("📈 PRODUCER STATISTICS:");
            log.info("   Total Sent: {}", sent);
            log.info("   Successful: {} ({:.1f}%)", success, successRate);
            log.info("   Failed: {} ({:.1f}%)", failure, failureRate);
            log.info("   ═══════════════════════════");
        }
    }

    /**
     * Get current producer statistics
     * @return Statistics summary
     */
    public String getStatistics() {
        long sent = sentCount.get();
        long success = successCount.get();
        long failure = failureCount.get();
        double successRate = sent > 0 ? (double) success / sent * 100 : 0.0;

        return String.format("Producer Stats - Sent: %d, Success: %d (%.1f%%), Failed: %d",
                sent, success, successRate, failure);
    }

    /**
     * Reset statistics counters
     */
    public void resetStatistics() {
        sentCount.set(0);
        successCount.set(0);
        failureCount.set(0);
        log.info("📊 Producer statistics reset");
    }
}