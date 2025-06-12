package com.toufik.trxgeneratorservice.mt103trx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class TransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MT103FileService mt103FileService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate, MT103FileService mt103FileService) {
        this.kafkaTemplate = kafkaTemplate;
        this.mt103FileService = mt103FileService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        log.info("TransactionProducer initialized with MT103FileService: {}", mt103FileService);
    }

    public void sendTransaction(TransactionWithMT103Event transactionWithMT103Event) {
        if (transactionWithMT103Event == null) {
            log.error("Received null TransactionWithMT103Event");
            return;
        }
        try {
            // Step 1: Save MT103 to file and store in map
            mt103FileService.saveMT103ToFile(
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    transactionWithMT103Event.getMt103Content()
            );
            log.info("Saved MT103 file for transaction: {} at path: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    mt103FileService.getMT103FilePath(transactionWithMT103Event.getTransaction().getTransactionId()));

            // Step 2: Send JSON message to Kafka
            kafkaTemplate.send(
                    "transaction_generator",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    transactionWithMT103Event
            );
            log.info("Sent transaction with MT103 info to Kafka: {}",
                    transactionWithMT103Event);

        } catch (IOException e) {
            log.error("Error saving MT103 file for transaction {}: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing transaction {}: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(), e.getMessage(), e);
        }
    }
}