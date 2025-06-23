package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighAmountDetectionServiceTest {

    @InjectMocks
    private HighAmountDetectionService highAmountDetectionService;

    private TransactionWithMT103Event mockEvent;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockEvent = mock(TransactionWithMT103Event.class);
        mockTransaction = mock(Transaction.class);
    }

    @Test
    void shouldDetectHighAmount_EUR() {

        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getAmount()).thenReturn(new BigDecimal("1500000"));
        when(mockTransaction.getCurrency()).thenReturn("EUR");
        boolean result = highAmountDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldDetectHighAmount_USD() {

        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getAmount()).thenReturn(new BigDecimal("1200000"));
        when(mockTransaction.getCurrency()).thenReturn("USD");
        boolean result = highAmountDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldNotDetectLowAmount_EUR() {

        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getAmount()).thenReturn(new BigDecimal("500000"));
        when(mockTransaction.getCurrency()).thenReturn("EUR");
        boolean result = highAmountDetectionService.isSuspicious(mockEvent);
        assertFalse(result);
    }

    @Test
    void shouldDetectExactThresholdAmount() {

        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getAmount()).thenReturn(new BigDecimal("1000000"));
        when(mockTransaction.getCurrency()).thenReturn("EUR");
        boolean result = highAmountDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldReturnCorrectRuleName() {
        assertEquals("HIGH_AMOUNT_DETECTION", highAmountDetectionService.getRuleName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = highAmountDetectionService.getDescription();
        assertTrue(description.contains("≥ 1,000,000"));
        assertTrue(description.contains("Large value transfers"));
    }
}