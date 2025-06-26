package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.entity.TransactionEntity;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.fraud.FraudAlertNotificationService;
import com.toufik.trxalertservice.fraud.service.FraudDetectionEngine;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionAlertConsumerService {

    private final FraudDetectionEngine fraudDetectionEngine;
    private final FraudAlertNotificationService fraudAlertNotificationService;
    private final TransactionService transactionService;

    @KafkaListener(
            topics = "transaction_alert",
            groupId = "transaction-alert-group",
            properties = {
                    "auto.offset.reset=latest"
            }
    )
    public void consumeTransactionAlert(@Payload TransactionWithMT103Event transactionWithMT103Event) {
        try {
            log.info("======================= ALERT SERVICE RECEIVED TRANSACTION =============================");
            log.info("ALERT SERVICE RECEIVED TRANSACTION ALERT {}", transactionWithMT103Event);
            List<FraudAlert> fraudAlerts = fraudDetectionEngine.detectFraud(transactionWithMT103Event);
            TransactionEntity savedTransaction = transactionService.saveTransaction(transactionWithMT103Event, fraudAlerts);
            if (!fraudAlerts.isEmpty()) {
                log.warn("Fraud detected for transaction: {} - Sending email notification",
                        savedTransaction.getTransactionId());
                fraudAlertNotificationService.sendFraudAlerts(fraudAlerts);
            }
            log.info("Transaction processing completed for: {}", savedTransaction.getTransactionId());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send fraud alert email for transaction: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(), e);
        } catch (Exception e) {
            log.error("Error processing transaction: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(), e);
        }
    }
}