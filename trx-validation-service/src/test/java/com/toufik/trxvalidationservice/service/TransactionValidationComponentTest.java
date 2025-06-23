package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.Transaction;
import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionValidationComponentTest {

    @Mock
    private KafkaTemplate<String, TransactionWithMT103Event> kafkaTemplate;

    @Mock
    private SendResult<String, TransactionWithMT103Event> sendResult;

    private TransactionConsumerService consumerService;
    private SimpleTransactionFilterService filterService;
    private TransactionProducerService producerService;

    @BeforeEach
    void setUp() {
        // Create the producer service with mocked KafkaTemplate
        producerService = new TransactionProducerService();
        ReflectionTestUtils.setField(producerService, "kafkaTemplate", kafkaTemplate);

        // Create the filter service with the producer service
        filterService = new SimpleTransactionFilterService();
        ReflectionTestUtils.setField(filterService, "producerService", producerService);

        // Create the consumer service with the filter service
        consumerService = new TransactionConsumerService();
        ReflectionTestUtils.setField(consumerService, "filterService", filterService);
    }

    @Test
    void shouldProcessValidTransactionAndSendAlert() {
        // Given - Valid MT103 transaction
        String validMT103 = "{1:F01COBADEFFXXX0}{2:I103UNCRITMMXXX0N}{3:{108:cd6d508c-5049-4a}}\n" +
                "{4:\n" +
                ":20:cd6d508c-5049-4a\n" +
                ":23B:CRED\n" +
                ":32A:250622EUR38329,19\n" +
                ":33B:EUR38329,19\n" +
                ":71A:SHA\n" +
                ":50K:/220576400523\n" +
                "10040000\n" +
                "456 Business Ave\n" +
                "Frankfurt, Germany\n" +
                ":52A:COBADEFF\n" +
                ":53B:/COBADEFF\n" +
                ":56A:UNCRITMM XXX\n" +
                ":57A:UNCRITMM\n" +
                ":59:/201093193710\n" +
                "02008\n" +
                "456 Business Ave\n" +
                "Milan, Italy\n" +
                ":70:Payment for services - TXN ID: cd6d508c - Cross-border transfer\n" +
                ":72:/INS/COBADEFF\n" +
                "}\n" +
                "{5:{MAC:9A90B885}{CHK:E065669BF6C5}}";

        Transaction transaction = Transaction.builder()
                .transactionId("cd6d508c-5049-4a")
                .amount(BigDecimal.valueOf(38329.19))
                .currency("EUR")
                .fromAccount("220576400523")
                .toAccount("201093193710")
                .timestamp(LocalDateTime.now())
                .build();

        TransactionWithMT103Event validEvent = TransactionWithMT103Event.builder()
                .transaction(transaction)
                .mt103Content(validMT103)
                .build();

        // Mock successful Kafka send for this specific test
        CompletableFuture<SendResult<String, TransactionWithMT103Event>> future =
                CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("transaction_alert"), eq("cd6d508c-5049-4a"), eq(validEvent)))
                .thenReturn(future);

        // When - Process the valid transaction
        consumerService.consume(validEvent, "transaction_generator", 0, 100L);

        // Then - Verify transaction was sent to alert topic
        verify(kafkaTemplate, times(1)).send(
                eq("transaction_alert"),
                eq("cd6d508c-5049-4a"),
                eq(validEvent)
        );
    }

    @Test
    void shouldProcessInvalidTransactionAndNotSendAlert() {
        // Given - Invalid MT103 transaction (missing mandatory field :23B:)
        String invalidMT103 = "{1:F01COBADEFFXXX0}{2:I103UNCRITMMXXX0N}{3:{108:invalid-txn-123}}\n" +
                "{4:\n" +
                ":20:invalid-txn-123\n" +
                ":32A:250622EUR1000,00\n" +
                ":50K:/123456789\n" +
                "Test Account\n" +
                ":52A:COBADEFF\n" +
                ":57A:UNCRITMM\n" +
                ":59:/987654321\n" +
                "Recipient Account\n" +
                "}\n" +
                "{5:{MAC:9A90B885}{CHK:E065669BF6C5}}";

        Transaction transaction = Transaction.builder()
                .transactionId("invalid-txn-123")
                .amount(BigDecimal.valueOf(1000.00))
                .currency("EUR")
                .fromAccount("123456789")
                .toAccount("987654321")
                .timestamp(LocalDateTime.now())
                .build();

        TransactionWithMT103Event invalidEvent = TransactionWithMT103Event.builder()
                .transaction(transaction)
                .mt103Content(invalidMT103)
                .build();

        consumerService.consume(invalidEvent, "transaction_generator", 0, 101L);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(TransactionWithMT103Event.class));
    }
}