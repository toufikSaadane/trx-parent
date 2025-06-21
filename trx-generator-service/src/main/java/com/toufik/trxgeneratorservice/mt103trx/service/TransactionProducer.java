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
    private final ObjectMapper objectMapper;

    @Autowired
    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void sendTransaction(TransactionWithMT103Event transactionWithMT103Event) {
        if (transactionWithMT103Event == null) {
            log.error("Received null TransactionWithMT103Event");
            return;
        }
        kafkaTemplate.send(
                "transaction_generator", transactionWithMT103Event.getTransaction().getTransactionId(), transactionWithMT103Event);
        log.info("Sent transaction with MT103 info to Kafka: {}", transactionWithMT103Event);

    }
}