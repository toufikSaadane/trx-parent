package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionConsumerService {

    @Autowired
    private TransactionFilterService filterService;

    @KafkaListener(topics = "transaction_generator",
            groupId = "transaction-validator-group",
            properties = {"auto.offset.reset=earliest"})
    public void consume(@Payload TransactionWithMT103Event event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {

        String transactionId = event.getTransaction().getTransactionId();

        try {
            log.info("Consumed transaction: {} from topic: {}, partition: {}, offset: {}",
                    transactionId, topic, partition, offset);

            filterService.process(event);

            log.info("Transaction {} processed successfully", transactionId);

        } catch (Exception e) {
            log.error("Error consuming transaction {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Failed to process transaction: " + transactionId, e);
        }
    }
}