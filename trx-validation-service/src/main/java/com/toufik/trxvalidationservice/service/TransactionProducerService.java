package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TransactionProducerService {

    private static final String TOPIC = "transactions_alert";

    @Autowired
    private KafkaTemplate<String, TransactionWithMT103Event> kafkaTemplate;

    public void sendTransactionAlert(TransactionWithMT103Event transactionWithMT103Event) {
        try {

            log.info("======================= PRODUCING TRANSACTION =============================");
            log.info("Kafka Message Details:");
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
            log.info("  Timestamp: {}", transactionWithMT103Event.getTransaction().getTimestamp());

            log.info("MT103 Content Preview:");

            String key = transactionWithMT103Event.getTransaction().getTransactionId();

            log.info("Sending transaction alert to topic '{}' with key '{}'", TOPIC, key);

            CompletableFuture<SendResult<String, TransactionWithMT103Event>> future =
                    kafkaTemplate.send(TOPIC, key, transactionWithMT103Event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent transaction alert for transaction '{}' with offset=[{}]",
                            key, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send transaction alert for transaction '{}': {}",
                            key, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error sending transaction alert for transaction '{}': {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    e.getMessage(), e);
            throw new RuntimeException("Failed to send transaction alert", e);
        }
    }
}