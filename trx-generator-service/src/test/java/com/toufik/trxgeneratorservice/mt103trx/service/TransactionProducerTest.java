package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionProducer producer;

    private TransactionWithMT103Event testEvent;

    @BeforeEach
    void setUp() {
        Transaction transaction = new Transaction(
                "test-transaction-id",
                "1234567890",
                "0987654321",
                new BigDecimal("1000.00"),
                "EUR",
                "DEUTDEFF",
                "BNPAFRPP",
                "Deutsche Bank",
                "BNP Paribas",
                LocalDateTime.now(),
                "PENDING"
        );

        testEvent = new TransactionWithMT103Event();
        testEvent.setTransaction(transaction);
        testEvent.setMt103Content("Mock MT103 Content");
    }

    @Test
    void testSendTransaction_Success() {
        producer.sendTransaction(testEvent);

        verify(kafkaTemplate).send(eq("transaction_generator"), eq("test-transaction-id"), eq(testEvent));
    }

    @Test
    void testSendTransaction_WithNullEvent() {
        producer.sendTransaction(null);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void testSendTransaction_CallsKafkaTemplate() {
        producer.sendTransaction(testEvent);

        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(TransactionWithMT103Event.class));
    }

    @Test
    void testSendTransaction_UsesCorrectTopic() {
        producer.sendTransaction(testEvent);

        verify(kafkaTemplate).send(eq("transaction_generator"), anyString(), any());
    }
}