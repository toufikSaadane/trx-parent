package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class TransactionProducerService {

    private static final String TOPIC = "transaction_alert";

    @Autowired
    private KafkaTemplate<String, TransactionWithMT103Event> kafkaTemplate;

    public void sendTransactionAlert(TransactionWithMT103Event event) {
        String transactionId = event.getTransaction().getTransactionId();

        try {
            validateEvent(event);
            CompletableFuture<SendResult<String, TransactionWithMT103Event>> future =
                    kafkaTemplate.send(TOPIC, transactionId, event);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Transaction {} sent successfully to topic: {}",
                            transactionId, TOPIC);
                } else {
                    log.error("Failed to send transaction {}: {}",
                            transactionId, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Exception sending transaction {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Kafka send failure for transaction: " + transactionId, e);
        }
    }

    private void validateEvent(TransactionWithMT103Event event) {

        if (event.getTransaction() == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (event.getTransaction().getTransactionId() == null ||
                event.getTransaction().getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
    }
}