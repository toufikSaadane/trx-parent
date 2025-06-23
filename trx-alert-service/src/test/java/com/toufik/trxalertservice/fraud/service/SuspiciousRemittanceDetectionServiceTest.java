package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuspiciousRemittanceDetectionServiceTest {

    @InjectMocks
    private SuspiciousRemittanceDetectionService suspiciousRemittanceDetectionService;

    private TransactionWithMT103Event createMockEventWithAmount(BigDecimal amount) {
        TransactionWithMT103Event mockEvent = mock(TransactionWithMT103Event.class);
        Transaction mockTransaction = mock(Transaction.class);
        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getAmount()).thenReturn(amount);
        when(mockTransaction.getTransactionId()).thenReturn("TXN123");
        return mockEvent;
    }

    @Test
    void shouldDetectSuspiciousAmount_999() {
        TransactionWithMT103Event mockEvent = createMockEventWithAmount(new BigDecimal("999"));
        boolean result = suspiciousRemittanceDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldDetectSuspiciousAmount_9999() {
        TransactionWithMT103Event mockEvent = createMockEventWithAmount(new BigDecimal("9999"));
        boolean result = suspiciousRemittanceDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldDetectSuspiciousAmount_99999() {
        TransactionWithMT103Event mockEvent = createMockEventWithAmount(new BigDecimal("99999"));
        boolean result = suspiciousRemittanceDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldDetectSuspiciousAmount_999999() {
        TransactionWithMT103Event mockEvent = createMockEventWithAmount(new BigDecimal("999999"));
        boolean result = suspiciousRemittanceDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldNotDetectNormalAmount() {
        TransactionWithMT103Event mockEvent = createMockEventWithAmount(new BigDecimal("1500"));
        boolean result = suspiciousRemittanceDetectionService.isSuspicious(mockEvent);
        assertFalse(result);
    }

    @Test
    void shouldReturnCorrectRuleName() {
        assertEquals("SUSPICIOUS_REMITTANCE_DETECTION", suspiciousRemittanceDetectionService.getRuleName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = suspiciousRemittanceDetectionService.getDescription();
        assertTrue(description.contains("suspicious pattern amounts"));
        assertTrue(description.contains("(999, 9999, 99999, 999999)"));
    }
}