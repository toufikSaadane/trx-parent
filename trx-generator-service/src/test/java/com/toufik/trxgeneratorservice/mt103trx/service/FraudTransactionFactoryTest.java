package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudTransactionFactoryTest {

    @Mock
    private BankDataService bankDataService;

    @InjectMocks
    private FraudTransactionFactory factory;

    @BeforeEach
    void setUp() {
        BankInfo mockBank = new BankInfo("DEUTDEFF", "DE", "Germany", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        when(bankDataService.getRandomBank()).thenReturn(mockBank);
        when(bankDataService.generateIBAN(any(), any())).thenReturn("DE89370400440532013000");
    }

    @Test
    void testCreateFraudTransaction_ReturnsValidTransaction() {
        Transaction transaction = factory.createFraudTransaction();

        assertNotNull(transaction);
        assertNotNull(transaction.getTransactionId());
        assertNotNull(transaction.getAmount());
        assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCreateFraudTransaction_GeneratesValidAmount() {
        Transaction transaction = factory.createFraudTransaction();

        assertNotNull(transaction.getAmount());
        assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0);
        System.out.println("Generated Amount: " + transaction.getAmount());
    }

    @Test
    void testCreateFraudTransaction_AppliesRandomPattern() {
        Transaction transaction1 = factory.createFraudTransaction();
        Transaction transaction2 = factory.createFraudTransaction();

        assertNotNull(transaction1);
        assertNotNull(transaction2);
        assertNotEquals(transaction1.getTransactionId(), transaction2.getTransactionId());
    }

    @Test
    void testCreateFraudTransaction_CallsBankDataService() {
        factory.createFraudTransaction();

        verify(bankDataService, atLeastOnce()).getRandomBank();
        verify(bankDataService, atLeastOnce()).generateIBAN(any(), any());
    }
}