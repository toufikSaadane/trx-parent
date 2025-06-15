package com.toufik.trxgeneratorservice.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import com.toufik.trxgeneratorservice.mt103trx.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "mt103.file.directory=${java.io.tmpdir}/test-mt103-transactions"
})
class MT103TransactionGeneratorComponentTest {

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private BankDataService bankDataService;

    // Changed back to @Autowired to use the real implementation
    @Autowired
    private TransactionProducer transactionProducer;

    @Qualifier("MT103MessageFormatter")
    @Autowired
    private MT103MessageFormatter mt103MessageFormatter;

    @Autowired
    private MT103FileService mt103FileService;

    @Autowired
    private TransactionGeneratorService transactionGeneratorService;

    private ObjectMapper objectMapper;
    private List<BankInfo> mockBanks;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup comprehensive mock bank data
        setupMockBankData();
    }

    private void setupMockBankData() {
        mockBanks = Arrays.asList(
                new BankInfo("CHASUS33XXX", "US", "JPMorgan Chase Bank", "021000021", "USD", "US", 28),
                new BankInfo("CITIUS33XXX", "US", "Citibank N.A.", "021000089", "USD", "US", 28),
                new BankInfo("DEUTDEFFXXX", "DE", "Deutsche Bank AG", "37040044", "EUR", "DE", 22),
                new BankInfo("HSBCGB2LXXX", "GB", "HSBC Bank plc", "400515", "GBP", "GB", 22),
                new BankInfo("BNPAFRPPXXX", "FR", "BNP Paribas", "30004", "EUR", "FR", 27),
                new BankInfo("UBSWCHZHXXX", "CH", "UBS Switzerland AG", "273", "CHF", "CH", 21)
        );

        // Configure mock to return different banks for from/to to ensure variety
        when(bankDataService.getRandomBank())
                .thenReturn(mockBanks.get(0))  // First call - from bank
                .thenReturn(mockBanks.get(1))  // Second call - to bank (different from first)
                .thenReturn(mockBanks.get(2))  // Third call - from bank
                .thenReturn(mockBanks.get(3))  // Fourth call - to bank
                .thenReturn(mockBanks.get(4))  // Fifth call - from bank
                .thenReturn(mockBanks.get(5)); // Sixth call - to bank

        // Mock IBAN generation
        when(bankDataService.generateIBAN(any(BankInfo.class), anyString()))
                .thenAnswer(invocation -> {
                    BankInfo bank = invocation.getArgument(0);
                    String accountNumber = invocation.getArgument(1);

                    // Return realistic IBANs based on country
                    return switch (bank.getCountryCode()) {
                        case "US" -> null; // US doesn't use IBAN
                        case "DE" -> "DE" + "89" + bank.getRoutingNumber() + accountNumber.substring(0, Math.min(10, accountNumber.length()));
                        case "GB" -> "GB" + "29" + bank.getRoutingNumber() + accountNumber.substring(0, Math.min(8, accountNumber.length()));
                        case "FR" -> "FR" + "14" + bank.getRoutingNumber() + accountNumber.substring(0, Math.min(11, accountNumber.length()));
                        case "CH" -> "CH" + "93" + bank.getRoutingNumber() + accountNumber.substring(0, Math.min(12, accountNumber.length()));
                        default -> bank.getCountryCode() + "12" + bank.getRoutingNumber() + accountNumber;
                    };
                });
    }

    @Test
    void testCompleteTransactionFlow_ShouldGenerateProcessAndSendTransaction() throws IOException {
        // Given
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TransactionWithMT103Event> messageCaptor = ArgumentCaptor.forClass(TransactionWithMT103Event.class);

        // When
        transactionGeneratorService.generateAndSendTransaction();

        // Then - Verify Kafka message was sent
        verify(kafkaTemplate, times(1)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                messageCaptor.capture()
        );

        // Verify Kafka topic and message structure
        assertEquals("transaction_generator", topicCaptor.getValue());
        assertNotNull(keyCaptor.getValue());

        // Verify the transaction event
        TransactionWithMT103Event sentTransaction = messageCaptor.getValue();
        assertNotNull(sentTransaction);
        assertNotNull(sentTransaction.getTransaction());
        assertNotNull(sentTransaction.getMt103Content());
        assertEquals(keyCaptor.getValue(), sentTransaction.getTransaction().getTransactionId());

        // Verify transaction details
        Transaction transaction = sentTransaction.getTransaction();
        assertValidTransaction(transaction);

        // Verify MT103 content structure
        assertValidMT103Content(sentTransaction.getMt103Content(), transaction);

        // Verify banks are different
        assertNotEquals(transaction.getFromBankSwift(), transaction.getToBankSwift());

        // Verify bank service was called appropriately
        verify(bankDataService, atLeast(2)).getRandomBank();
        verify(bankDataService, times(2)).generateIBAN(any(BankInfo.class), anyString());
    }

    @Test
    void testTransactionProducer_ShouldHandleCompleteWorkflow() throws IOException {
        // Given
        TransactionWithMT103Event transaction = createSampleTransactionWithMT103();
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TransactionWithMT103Event> messageCaptor = ArgumentCaptor.forClass(TransactionWithMT103Event.class);

        // When
        transactionProducer.sendTransaction(transaction);

        // Then
        verify(kafkaTemplate, times(1)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals("transaction_generator", topicCaptor.getValue());
        assertEquals(transaction.getTransaction().getTransactionId(), keyCaptor.getValue());

        // Verify the message structure
        TransactionWithMT103Event sentTransaction = messageCaptor.getValue();
        assertNotNull(sentTransaction);
        assertEquals(transaction.getTransaction().getTransactionId(), sentTransaction.getTransaction().getTransactionId());
        assertEquals(transaction.getTransaction().getAmount(), sentTransaction.getTransaction().getAmount());
        assertEquals(transaction.getTransaction().getCurrency(), sentTransaction.getTransaction().getCurrency());
        assertNotNull(sentTransaction.getMt103Content());

        // Verify file was created
        String expectedFilePath = mt103FileService.getMT103FilePath(transaction.getTransaction().getTransactionId());
        assertNotNull(expectedFilePath);
    }

    @Test
    void testMT103MessageFormatter_ShouldFormatCorrectlyForDifferentScenarios() {
        // Test domestic transaction
        Transaction domesticTransaction = createSampleTransaction("CHASUS33XXX", "CITIUS33XXX", "USD");
        String mt103Content = mt103MessageFormatter.formatToMT103(domesticTransaction);
        assertValidMT103Content(mt103Content, domesticTransaction);

        // Test cross-border transaction
        Transaction crossBorderTransaction = createSampleTransaction("CHASUS33XXX", "DEUTDEFFXXX", "USD");
        String crossBorderMT103 = mt103MessageFormatter.formatToMT103(crossBorderTransaction);
        assertValidMT103Content(crossBorderMT103, crossBorderTransaction);

        // Cross-border should include intermediary bank field (56A)
        assertTrue(crossBorderMT103.contains(":56A:"), "Cross-border transaction should contain intermediary bank field");

        // Test European transaction (should use EUR)
        Transaction eurTransaction = createSampleTransaction("DEUTDEFFXXX", "BNPAFRPPXXX", "EUR");
        String eurMT103 = mt103MessageFormatter.formatToMT103(eurTransaction);
        assertValidMT103Content(eurMT103, eurTransaction);
        assertTrue(eurMT103.contains("EUR"), "European transaction should use EUR currency");
    }

    @Test
    void testGeneratedTransaction_ShouldHaveValidStructureAndConstraints() {
        // Given - ArgumentCaptor to capture the transaction sent to Kafka
        ArgumentCaptor<TransactionWithMT103Event> kafkaCaptor = ArgumentCaptor.forClass(TransactionWithMT103Event.class);

        // When
        transactionGeneratorService.generateAndSendTransaction();

        // Then - Verify Kafka was called and capture the transaction
        verify(kafkaTemplate, times(1)).send(
                anyString(),
                anyString(),
                kafkaCaptor.capture()
        );

        TransactionWithMT103Event generated = kafkaCaptor.getValue();
        Transaction transaction = generated.getTransaction();

        // Verify complete transaction structure
        assertValidTransaction(transaction);

        // Verify business constraints
        assertTrue(transaction.getFromAccount().matches("\\d{10,12}"),
                "From account should be 10-12 digits");
        assertTrue(transaction.getToAccount().matches("\\d{10,12}"),
                "To account should be 10-12 digits");

        // Verify amount ranges (based on the generator logic)
        assertTrue(transaction.getAmount().compareTo(BigDecimal.valueOf(10.0)) >= 0,
                "Amount should be at least 10.0");
        assertTrue(transaction.getAmount().compareTo(BigDecimal.valueOf(100000.0)) <= 0,
                "Amount should be at most 100000.0");

        // Verify different banks
        assertNotEquals(transaction.getFromBankSwift(), transaction.getToBankSwift(),
                "From and To banks should be different");

        // Verify MT103 content exists and is valid
        assertNotNull(generated.getMt103Content());
        assertValidMT103Content(generated.getMt103Content(), transaction);
    }

    @Test
    void testMultipleTransactionGeneration_ShouldCreateVariedTransactions() {
        // Given - Generate multiple transactions to test variety
        int transactionCount = 3;
        ArgumentCaptor<TransactionWithMT103Event> captor = ArgumentCaptor.forClass(TransactionWithMT103Event.class);

        // When
        for (int i = 0; i < transactionCount; i++) {
            transactionGeneratorService.generateAndSendTransaction();
        }

        // Then
        verify(kafkaTemplate, times(transactionCount)).send(
                anyString(),
                anyString(),
                captor.capture()
        );

        List<TransactionWithMT103Event> generatedTransactions = captor.getAllValues();
        assertEquals(transactionCount, generatedTransactions.size());

        // Verify each transaction is valid and unique
        for (int i = 0; i < transactionCount; i++) {
            Transaction transaction = generatedTransactions.get(i).getTransaction();
            assertValidTransaction(transaction);

            // Verify uniqueness (transaction IDs should be different)
            for (int j = i + 1; j < transactionCount; j++) {
                assertNotEquals(transaction.getTransactionId(),
                        generatedTransactions.get(j).getTransaction().getTransactionId(),
                        "Transaction IDs should be unique");
            }
        }

        // Verify bank service was called appropriately for all transactions
        verify(bankDataService, times(transactionCount * 2)).getRandomBank();
    }

    @Test
    void testTransactionProducer_ShouldHandleNullTransaction() {
        // When & Then - Should handle null gracefully without throwing exception
        assertDoesNotThrow(() -> transactionProducer.sendTransaction(null));

        // Note: We don't verify kafkaTemplate interaction here since null handling
        // might not result in a Kafka send operation
    }

    @Test
    void testMT103FileService_ShouldCreateAndTrackFiles() throws IOException {
        // Given
        String transactionId = "test-transaction-123";
        String mt103Content = "Sample MT103 content for testing";

        // When
        mt103FileService.saveMT103ToFile(transactionId, mt103Content);

        // Then
        String filePath = mt103FileService.getMT103FilePath(transactionId);
        assertNotNull(filePath, "File path should be stored and retrievable");

        // Verify file exists and contains correct content
        Path actualFile = Path.of(filePath);
        assertTrue(Files.exists(actualFile), "MT103 file should exist");

        String fileContent = Files.readString(actualFile);
        assertEquals(mt103Content, fileContent, "File content should match input");
    }

    // Helper methods
    private void assertValidTransaction(Transaction transaction) {
        assertNotNull(transaction.getTransactionId(), "Transaction ID should not be null");
        assertNotNull(transaction.getFromAccount(), "From account should not be null");
        assertNotNull(transaction.getToAccount(), "To account should not be null");
        assertNotNull(transaction.getAmount(), "Amount should not be null");
        assertNotNull(transaction.getCurrency(), "Currency should not be null");
        assertNotNull(transaction.getFromBankSwift(), "From bank SWIFT should not be null");
        assertNotNull(transaction.getToBankSwift(), "To bank SWIFT should not be null");
        assertNotNull(transaction.getFromBankName(), "From bank name should not be null");
        assertNotNull(transaction.getToBankName(), "To bank name should not be null");
        assertNotNull(transaction.getTimestamp(), "Timestamp should not be null");
        assertNotNull(transaction.getStatus(), "Status should not be null");

        // Verify status is one of the expected values
        assertTrue(Arrays.asList("PENDING", "COMPLETED", "FAILED", "PROCESSING").contains(transaction.getStatus()),
                "Status should be one of the expected values");

        // Verify amount is positive
        assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0, "Amount should be positive");

        // Verify currency format
        assertTrue(transaction.getCurrency().matches("[A-Z]{3}"), "Currency should be 3 uppercase letters");

        // Verify SWIFT codes format
        assertTrue(transaction.getFromBankSwift().matches("[A-Z]{4}[A-Z]{2}[0-9A-Z]{2}([0-9A-Z]{3})?"),
                "From bank SWIFT should be valid format");
        assertTrue(transaction.getToBankSwift().matches("[A-Z]{4}[A-Z]{2}[0-9A-Z]{2}([0-9A-Z]{3})?"),
                "To bank SWIFT should be valid format");
    }

    private void assertValidMT103Content(String mt103Content, Transaction transaction) {
        assertNotNull(mt103Content, "MT103 content should not be null");
        assertFalse(mt103Content.trim().isEmpty(), "MT103 content should not be empty");

        // Verify essential MT103 structure
        assertTrue(mt103Content.contains("{1:F01"), "Should contain basic header block");
        assertTrue(mt103Content.contains("{2:I103"), "Should contain application header");
        assertTrue(mt103Content.contains("{3:"), "Should contain user header");
        assertTrue(mt103Content.contains("{4:"), "Should contain text block");
        assertTrue(mt103Content.contains("{5:"), "Should contain trailer block");

        // Verify transaction data is included
        assertTrue(mt103Content.contains(transaction.getFromBankSwift()),
                "Should contain from bank SWIFT");
        assertTrue(mt103Content.contains(transaction.getToBankSwift()),
                "Should contain to bank SWIFT");
        assertTrue(mt103Content.contains(transaction.getCurrency()),
                "Should contain currency");

        // Verify essential MT103 fields
        assertTrue(mt103Content.contains(":20:"), "Should contain transaction reference field");
        assertTrue(mt103Content.contains(":23B:"), "Should contain bank operation code");
        assertTrue(mt103Content.contains(":32A:"), "Should contain value date and amount");
        assertTrue(mt103Content.contains(":50K:"), "Should contain ordering customer");
        assertTrue(mt103Content.contains(":59:"), "Should contain beneficiary customer");
        assertTrue(mt103Content.contains(":70:"), "Should contain remittance information");
    }

    private Transaction createSampleTransaction() {
        return createSampleTransaction("CHASUS33XXX", "CITIUS33XXX", "USD");
    }

    private Transaction createSampleTransaction(String fromSwift, String toSwift, String currency) {
        Transaction transaction = new Transaction(
                "test-transaction-123",
                "123456789012",
                "987654321098",
                BigDecimal.valueOf(1000.50),
                currency,
                fromSwift,
                toSwift,
                "JPMorgan Chase Bank",
                "Citibank N.A.",
                LocalDateTime.now(),
                "PENDING"
        );

        // Set country codes based on SWIFT codes
        transaction.setFromCountryCode(fromSwift.substring(4, 6));
        transaction.setToCountryCode(toSwift.substring(4, 6));

        // Set IBANs if applicable
        if (!"US".equals(transaction.getFromCountryCode())) {
            transaction.setFromIBAN("DE89370400440532013000");
        }
        if (!"US".equals(transaction.getToCountryCode())) {
            transaction.setToIBAN("GB29NWBK60161331926819");
        }

        return transaction;
    }

    private TransactionWithMT103Event createSampleTransactionWithMT103() {
        Transaction transaction = createSampleTransaction();
        String mt103Content = mt103MessageFormatter.formatToMT103(transaction);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(transaction);
        result.setMt103Content(mt103Content);
        return result;
    }
}
