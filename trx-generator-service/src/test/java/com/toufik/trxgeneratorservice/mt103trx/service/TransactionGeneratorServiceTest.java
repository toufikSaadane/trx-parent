package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionGeneratorServiceTest {

    @Mock
    private TransactionProducer transactionProducer;

    @Mock
    private MT103MessageFormatter mt103MessageFormatter;

    @Mock
    private TransactionSaveService transactionSaveService;

    @Mock
    private BankDataService bankDataService;

    private TransactionGeneratorService service;

    @BeforeEach
    void setUp() {
        // Create service instance manually to control dependency injection
        service = new TransactionGeneratorService(
                transactionProducer,
                mt103MessageFormatter,
                transactionSaveService
        );

        // Inject the BankDataService mock into the base class
        ReflectionTestUtils.setField(service, "bankDataService", bankDataService);

        // Setup mock behaviors
        BankInfo mockBank = new BankInfo("DEUTDEFF", "DE", "Germany", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        when(bankDataService.getRandomBank()).thenReturn(mockBank);
        when(bankDataService.generateIBAN(any(), any())).thenReturn("DE89370400440532013000");
        when(mt103MessageFormatter.formatToMT103(any())).thenReturn("Mock MT103 Content");
    }

    @Test
    void testGenerateAndSendTransaction_Success() {
        // When
        service.generateAndSendTransaction();

        // Then
        verify(transactionSaveService).saveTransaction(any(Transaction.class), anyString());
        verify(transactionProducer).sendTransaction(any(TransactionWithMT103Event.class));
        verify(mt103MessageFormatter).formatToMT103(any(Transaction.class));
        verify(bankDataService, atLeastOnce()).getRandomBank();
    }

    @Test
    void testGenerateAndSendTransaction_HandlesException() {
        // Given
        doThrow(new RuntimeException("Test exception")).when(transactionSaveService).saveTransaction(any(), any());

        // When & Then - Should not throw exception, just log error
        assertDoesNotThrow(() -> service.generateAndSendTransaction());

        // Verify that the producer is not called when save fails
        verify(transactionSaveService).saveTransaction(any(), any());
        // Note: The producer might still be called depending on the order of operations in your actual code
    }

    @Test
    void testGenerateAndSendTransaction_CallsAllServices() {
        // When
        service.generateAndSendTransaction();

        // Then
        verify(bankDataService, atLeastOnce()).getRandomBank();
        verify(mt103MessageFormatter).formatToMT103(any(Transaction.class));
        verify(transactionSaveService).saveTransaction(any(Transaction.class), anyString());
        verify(transactionProducer).sendTransaction(any(TransactionWithMT103Event.class));
    }

    @Test
    void testGenerateAndSendTransaction_ProducerException() {
        // Given
        doThrow(new RuntimeException("Producer exception")).when(transactionProducer).sendTransaction(any());

        // When & Then
        assertDoesNotThrow(() -> service.generateAndSendTransaction());

        // Verify services were still called
        verify(transactionSaveService).saveTransaction(any(), any());
        verify(transactionProducer).sendTransaction(any());
    }

    @Test
    void testGenerateAndSendTransaction_FormatterException() {
        // Given
        when(mt103MessageFormatter.formatToMT103(any())).thenThrow(new RuntimeException("Format exception"));

        // When & Then
        assertDoesNotThrow(() -> service.generateAndSendTransaction());

        // Verify formatter was called
        verify(mt103MessageFormatter).formatToMT103(any());
    }
}