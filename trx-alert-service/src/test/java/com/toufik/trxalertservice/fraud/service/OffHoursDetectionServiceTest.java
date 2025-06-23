package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class OffHoursDetectionServiceTest {

    @InjectMocks
    private OffHoursDetectionService offHoursDetectionService;

    private TransactionWithMT103Event mockEvent;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockEvent = mock(TransactionWithMT103Event.class);
        mockTransaction = mock(Transaction.class);
        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getTransactionId()).thenReturn("TXN123");
    }

    @Test
    void shouldDetectOffHoursTransaction_LateNight() {
        LocalDateTime offHoursTime = LocalDateTime.of(2024, 1, 1, 2, 30);
        when(mockTransaction.getTimestamp()).thenReturn(offHoursTime);
        boolean result = offHoursDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldDetectOffHoursTransaction_EarlyMorning() {
        LocalDateTime offHoursTime = LocalDateTime.of(2024, 1, 1, 5, 30);
        when(mockTransaction.getTimestamp()).thenReturn(offHoursTime);
        boolean result = offHoursDetectionService.isSuspicious(mockEvent);
        assertTrue(result);
    }

    @Test
    void shouldNotDetectOffHoursTransaction_BusinessHours() {
        LocalDateTime businessHoursTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        when(mockTransaction.getTimestamp()).thenReturn(businessHoursTime);
        boolean result = offHoursDetectionService.isSuspicious(mockEvent);
        assertFalse(result);
    }
}