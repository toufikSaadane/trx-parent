package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.model.FraudDetectionResult;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionAlertConsumerService {

    private final FraudDetectionService fraudDetectionService;
    private final FraudFileWriterService fraudFileWriterService;
    private final SwiftFileWriterService swiftFileWriterService;

    @Autowired
    public TransactionAlertConsumerService(FraudDetectionService fraudDetectionService,
                                           FraudFileWriterService fraudFileWriterService,
                                           SwiftFileWriterService swiftFileWriterService) {
        this.fraudDetectionService = fraudDetectionService;
        this.fraudFileWriterService = fraudFileWriterService;
        this.swiftFileWriterService = swiftFileWriterService;
    }

    @KafkaListener(
            topics = "transaction_alert",
            groupId = "transaction-alert-group",
            properties = {
                    "auto.offset.reset=latest"  // Force ignore old messages
            }
    )
    public void consumeTransactionAlert(
            @Payload TransactionWithMT103Event transactionWithMT103Event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        String transactionId = transactionWithMT103Event.getTransaction().getTransactionId();

        try {
            logTransactionDetails(transactionWithMT103Event, topic);

            // Perform fraud detection
            FraudDetectionResult fraudResult = fraudDetectionService.detectFraud(
                    transactionWithMT103Event.getTransaction());

            // Write transaction to appropriate fraud detection file
            fraudFileWriterService.writeTransactionToFile(
                    transactionWithMT103Event.getTransaction(), fraudResult);

            // Write MT103 content to SWIFT file
            swiftFileWriterService.writeSwiftFile(transactionWithMT103Event, fraudResult);

            logFraudDetectionResult(fraudResult);

            log.debug("Transaction alert {} processed successfully", transactionId);

        } catch (Exception e) {
            log.error("Error processing transaction alert {}: {}", transactionId, e.getMessage(), e);
            // Could add dead letter queue or retry logic here
        }
    }

    private void logTransactionDetails(TransactionWithMT103Event event, String topic) {
        log.info("======================= ALERT SERVICE RECEIVED TRANSACTION =============================");
        log.info("Kafka Message Details:");
        log.info("  Topic: {}", topic);

        log.info("Transaction Details:");
        log.info("  Transaction ID: {}", event.getTransaction().getTransactionId());
        log.info("  From Account: {}", event.getTransaction().getFromAccount());
        log.info("  To Account: {}", event.getTransaction().getToAccount());
        log.info("  Amount: {} {}",
                event.getTransaction().getAmount(),
                event.getTransaction().getCurrency());
        log.info("  From Bank: {}", event.getTransaction().getFromBankName());
        log.info("  To Bank: {}", event.getTransaction().getToBankName());
        log.info("  Status: {}", event.getTransaction().getStatus());
    }

    private void logFraudDetectionResult(FraudDetectionResult fraudResult) {
        log.info("Fraud Detection Result:");
        log.info("  Is Fraudulent: {}", fraudResult.isFraudulent());
        log.info("  Risk Score: {}", fraudResult.getRiskScore());
        log.info("  Risk Level: {}", fraudResult.getRiskLevel());
        log.info("  Alert Count: {}", fraudResult.getAlerts() != null ? fraudResult.getAlerts().size() : 0);
        log.info("==================================================================");
    }
}