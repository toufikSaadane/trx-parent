package com.toufik.trxvalidationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toufik.trxvalidationservice.model.Transaction;
import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import com.toufik.trxvalidationservice.service.SimpleTransactionFilterService;
import com.toufik.trxvalidationservice.service.TransactionConsumerService;
import com.toufik.trxvalidationservice.service.TransactionProducerService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"transaction_generator", "transaction_alert"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=test-group"
})
@DirtiesContext
class TransactionValidationIntegrationTest {

//    @Autowired
//    private EmbeddedKafkaBroker embeddedKafkaBroker;
//
//    @Autowired
//    private TransactionConsumerService consumerService;
//
//    @Autowired
//    private SimpleTransactionFilterService filterService;
//
//    @Autowired
//    private TransactionProducerService producerService;
//
//    private Producer<String, TransactionWithMT103Event> producer;
//    private Consumer<String, TransactionWithMT103Event> alertConsumer;
//    private ObjectMapper objectMapper;
//
//    private String validMT103Content;
//    private String invalidMT103Content;
//
//    @BeforeEach
//    void setUp() {
//        objectMapper = new ObjectMapper();
//
//        // Setup producer for sending messages to transaction_generator topic
//        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
//        producerProps.put("key.serializer", StringSerializer.class);
//        producerProps.put("value.serializer", JsonSerializer.class);
//        producer = new DefaultKafkaProducerFactory<String, TransactionWithMT103Event>(producerProps).createProducer();
//
//        // Setup consumer for reading from transaction_alert topic
//        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-alert-group", "true", embeddedKafkaBroker);
//        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionWithMT103Event.class);
//
//        alertConsumer = new DefaultKafkaConsumerFactory<String, TransactionWithMT103Event>(consumerProps).createConsumer();
//        alertConsumer.subscribe(Collections.singletonList("transaction_alert"));
//
//        // Valid MT103 content
//        validMT103Content = "{1:F01COBADEFFXXX0}{2:I103UNCRITMMXXX0N}{3:{108:cd6d508c-5049-4a}}\n" +
//                "{4:\n" +
//                ":20:cd6d508c-5049-4a\n" +
//                ":23B:CRED\n" +
//                ":32A:250622EUR38329,19\n" +
//                ":33B:EUR38329,19\n" +
//                ":71A:SHA\n" +
//                ":50K:/220576400523\n" +
//                "10040000\n" +
//                "456 Business Ave\n" +
//                "Frankfurt, Germany\n" +
//                ":52A:COBADEFF\n" +
//                ":53B:/COBADEFF\n" +
//                ":56A:UNCRITMM XXX\n" +
//                ":57A:UNCRITMM\n" +
//                ":59:/201093193710\n" +
//                "02008\n" +
//                "456 Business Ave\n" +
//                "Milan, Italy\n" +
//                ":70:Payment for services - TXN ID: cd6d508c - Cross-border transfer\n" +
//                ":72:/INS/COBADEFF\n" +
//                "}\n" +
//                "{5:{MAC:9A90B885}{CHK:E065669BF6C5}}";
//
//        // Invalid MT103 content (missing mandatory field :20:)
//        invalidMT103Content = "{1:F01COBADEFFXXX0}{2:I103UNCRITMMXXX0N}{3:{108:cd6d508c-5049-4a}}\n" +
//                "{4:\n" +
//                ":23B:CRED\n" +
//                ":32A:250622EUR38329,19\n" +
//                ":33B:EUR38329,19\n" +
//                ":71A:SHA\n" +
//                "}\n" +
//                "{5:{MAC:9A90B885}{CHK:E065669BF6C5}}";
//    }
//
//    @Test
//    void testCompleteFlow_ValidTransaction_ShouldProduceAlert() throws Exception {
//        // Given
//        String transactionId = "valid-txn-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, validMT103Content);
//
//        // When - Send message to transaction_generator topic
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then - Verify message is consumed and alert is produced
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(10));
//
//        assertFalse(records.isEmpty(), "Should receive alert for valid transaction");
//
//        ConsumerRecord<String, TransactionWithMT103Event> alertRecord = records.iterator().next();
//        assertEquals(transactionId, alertRecord.key());
//        assertEquals(transactionId, alertRecord.value().getTransaction().getTransactionId());
//        assertEquals(validMT103Content, alertRecord.value().getMt103Content());
//    }
//
//    @Test
//    void testCompleteFlow_InvalidTransaction_ShouldNotProduceAlert() throws Exception {
//        // Given
//        String transactionId = "invalid-txn-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, invalidMT103Content);
//
//        // When - Send message to transaction_generator topic
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then - Verify no alert is produced
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(5));
//
//        assertTrue(records.isEmpty(), "Should not receive alert for invalid transaction");
//    }
//
//    @Test
//    void testCompleteFlow_MultipleTransactions_ValidAndInvalid() throws Exception {
//        // Given
//        String validTxnId = "valid-multi-" + UUID.randomUUID().toString();
//        String invalidTxnId = "invalid-multi-" + UUID.randomUUID().toString();
//
//        TransactionWithMT103Event validEvent = createTransactionEvent(validTxnId, validMT103Content);
//        TransactionWithMT103Event invalidEvent = createTransactionEvent(invalidTxnId, invalidMT103Content);
//
//        // When - Send both messages
//        producer.send(new ProducerRecord<>("transaction_generator", validTxnId, validEvent)).get(10, TimeUnit.SECONDS);
//        producer.send(new ProducerRecord<>("transaction_generator", invalidTxnId, invalidEvent)).get(10, TimeUnit.SECONDS);
//
//        // Then - Should only receive alert for valid transaction
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(10));
//
//        assertEquals(1, records.count(), "Should receive exactly one alert");
//
//        ConsumerRecord<String, TransactionWithMT103Event> alertRecord = records.iterator().next();
//        assertEquals(validTxnId, alertRecord.key());
//        assertEquals(validTxnId, alertRecord.value().getTransaction().getTransactionId());
//    }
//
//    @Test
//    void testCompleteFlow_InvalidBICTransaction_ShouldNotProduceAlert() throws Exception {
//        // Given - Invalid BIC format
//        String invalidBicContent = validMT103Content.replace(":52A:COBADEFF", ":52A:INVALID");
//        String transactionId = "invalid-bic-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, invalidBicContent);
//
//        // When
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(5));
//
//        assertTrue(records.isEmpty(), "Should not receive alert for invalid BIC transaction");
//    }
//
//    @Test
//    void testCompleteFlow_InvalidDateTransaction_ShouldNotProduceAlert() throws Exception {
//        // Given - Invalid date format
//        String invalidDateContent = validMT103Content.replace(":32A:250622EUR38329,19", ":32A:25062XEUR38329,19");
//        String transactionId = "invalid-date-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, invalidDateContent);
//
//        // When
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(5));
//
//        assertTrue(records.isEmpty(), "Should not receive alert for invalid date transaction");
//    }
//
//    @Test
//    void testCompleteFlow_InvalidAmountTransaction_ShouldNotProduceAlert() throws Exception {
//        // Given - Invalid amount format
//        String invalidAmountContent = validMT103Content.replace(":32A:250622EUR38329,19", ":32A:250622EURABCD,19");
//        String transactionId = "invalid-amount-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, invalidAmountContent);
//
//        // When
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(5));
//
//        assertTrue(records.isEmpty(), "Should not receive alert for invalid amount transaction");
//    }
//
//    @Test
//    void testCompleteFlow_MissingStructureBlocks_ShouldNotProduceAlert() throws Exception {
//        // Given - Missing block 1
//        String missingBlockContent = "{2:I103UNCRITMMXXX0N}{3:{108:cd6d508c-5049-4a}}\n" +
//                "{4::20:test:23B:CRED:32A:250622EUR38329,19}\n" +
//                "{5:{MAC:9A90B885}{CHK:E065669BF6C5}}";
//        String transactionId = "missing-block-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, missingBlockContent);
//
//        // When
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(5));
//
//        assertTrue(records.isEmpty(), "Should not receive alert for transaction missing structure blocks");
//    }
//
//    @Test
//    void testCompleteFlow_ValidTransactionWithDifferentBIC_ShouldProduceAlert() throws Exception {
//        // Given - Valid transaction with 11-character BIC
//        String validBicContent = validMT103Content.replace(":52A:COBADEFF", ":52A:COBADEFFXXX");
//        String transactionId = "valid-bic11-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, validBicContent);
//
//        // When
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(10));
//
//        assertFalse(records.isEmpty(), "Should receive alert for valid transaction with 11-char BIC");
//
//        ConsumerRecord<String, TransactionWithMT103Event> alertRecord = records.iterator().next();
//        assertEquals(transactionId, alertRecord.key());
//    }
//
//    @Test
//    void testCompleteFlow_ValidTransactionWithPeriodAmount_ShouldProduceAlert() throws Exception {
//        // Given - Valid transaction with period in amount
//        String validAmountContent = validMT103Content.replace(":32A:250622EUR38329,19", ":32A:250622EUR38329.19");
//        String transactionId = "valid-period-" + UUID.randomUUID().toString();
//        TransactionWithMT103Event event = createTransactionEvent(transactionId, validAmountContent);
//
//        // When
//        ProducerRecord<String, TransactionWithMT103Event> record =
//                new ProducerRecord<>("transaction_generator", transactionId, event);
//        producer.send(record).get(10, TimeUnit.SECONDS);
//
//        // Then
//        ConsumerRecords<String, TransactionWithMT103Event> records =
//                alertConsumer.poll(Duration.ofSeconds(10));
//
//        assertFalse(records.isEmpty(), "Should receive alert for valid transaction with period amount");
//
//        ConsumerRecord<String, TransactionWithMT103Event> alertRecord = records.iterator().next();
//        assertEquals(transactionId, alertRecord.key());
//    }
//
//    private TransactionWithMT103Event createTransactionEvent(String transactionId, String mt103Content) {
//        Transaction transaction = new Transaction();
//        transaction.setTransactionId(transactionId);
//
//        TransactionWithMT103Event event = new TransactionWithMT103Event();
//        event.setTransaction(transaction);
//        event.setMt103Content(mt103Content);
//
//        return event;
//    }
}