package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionConsumerService {

    @Autowired
    private TransactionProducerService transactionProducerService;

    @KafkaListener(
            topics = "transaction_generator",
            groupId = "transaction-validator-group"
    )
    public void consumeTransaction(
            @Payload TransactionWithMT103Event transactionWithMT103Event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        try {
            log.info("======================= CONSUMED TRANSACTION =============================");
            log.info("Kafka Message Details:");
            log.info("  Topic: {}", topic);
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
            String mt103Content = transactionWithMT103Event.getMt103Content();
            if (mt103Content != null) {
                String[] lines = mt103Content.split("\n");
                for (int i = 0; i < Math.min(5, lines.length); i++) {
                    log.info("  {}", lines[i]);
                }
                if (lines.length > 5) {
                    log.info("  ... ({} more lines)", lines.length - 5);
                }
            } else {
                log.warn("  MT103 content is null");
            }
            log.info("==================================================================");
            transactionProducerService.sendTransactionAlert(transactionWithMT103Event);
            log.debug("Transaction {} processed and forwarded successfully",
                    transactionWithMT103Event.getTransaction().getTransactionId());

        } catch (Exception e) {
            log.error("Error processing transaction {}: {}",
                    transactionWithMT103Event.getTransaction().getTransactionId(),
                    e.getMessage(), e);
        }
    }
}