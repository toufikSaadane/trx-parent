package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.fraud.FraudAlertNotificationService;
import com.toufik.trxalertservice.fraud.service.FraudDetectionEngine;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionAlertConsumerService {

    private final FraudDetectionEngine fraudDetectionEngine;
    private final FraudAlertNotificationService fraudAlertNotificationService;

    @KafkaListener(
            topics = "transaction_alert",
            groupId = "transaction-alert-group",
            properties = {
                    "auto.offset.reset=latest"
            }
    )
    public void consumeTransactionAlert(
            @Payload TransactionWithMT103Event transactionWithMT103Event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        String transactionId = transactionWithMT103Event.getTransaction().getTransactionId();

        try {
            logTransactionDetails(transactionWithMT103Event, topic);

            // Execute fraud detection
            List<FraudAlert> fraudAlerts = fraudDetectionEngine.detectFraud(transactionWithMT103Event);

            // Send notifications if fraud detected
            if (!fraudAlerts.isEmpty()) {
                fraudAlertNotificationService.sendFraudAlerts(fraudAlerts);
            }

            log.debug("Transaction alert {} processed successfully", transactionId);

        } catch (Exception e) {
            log.error("Error processing transaction alert {}: {}", transactionId, e.getMessage(), e);
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
        log.info("  From Country: {}", event.getTransaction().getFromCountryCode());
        log.info("  To Country: {}", event.getTransaction().getToCountryCode());
        log.info("  Status: {}", event.getTransaction().getStatus());
        log.info("  Timestamp: {}", event.getTransaction().getTimestamp());
    }
}