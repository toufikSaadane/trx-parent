package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighRiskCountryDetectionServiceTest {

    @InjectMocks
    private HighRiskCountryDetectionService highRiskCountryDetectionService;

    private TransactionWithMT103Event mockEvent;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockEvent = mock(TransactionWithMT103Event.class);
        mockTransaction = mock(Transaction.class);
//        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
//        when(mockTransaction.getTransactionId()).thenReturn("TXN123");
    }

    @Test
    void shouldDetectHighRiskDestination() {
        // Given
        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getFromCountryCode()).thenReturn("US");
        when(mockTransaction.getToCountryCode()).thenReturn("AF");

        // When
        boolean result = highRiskCountryDetectionService.isSuspicious(mockEvent);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldDetectHighRiskSource() {
        // Given
        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getFromCountryCode()).thenReturn("IR"); // Iran
        when(mockTransaction.getToCountryCode()).thenReturn("US");

        // When
        boolean result = highRiskCountryDetectionService.isSuspicious(mockEvent);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldDetectBothHighRiskSourceAndDestination() {
        // Given
        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getFromCountryCode()).thenReturn("KP"); // North Korea
        when(mockTransaction.getToCountryCode()).thenReturn("SY"); // Syria

        // When
        boolean result = highRiskCountryDetectionService.isSuspicious(mockEvent);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotDetectLowRiskCountries() {
        // Given
        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getFromCountryCode()).thenReturn("US");
        when(mockTransaction.getToCountryCode()).thenReturn("GB");

        // When
        boolean result = highRiskCountryDetectionService.isSuspicious(mockEvent);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldDetectAllHighRiskCountries() {
        String[] highRiskCountries = {"AF", "IR", "KP", "MM", "SY", "YE"};

        for (String country : highRiskCountries) {
            // Given
            when(mockEvent.getTransaction()).thenReturn(mockTransaction);
            when(mockTransaction.getFromCountryCode()).thenReturn("US");
            when(mockTransaction.getToCountryCode()).thenReturn(country);

            // When
            boolean result = highRiskCountryDetectionService.isSuspicious(mockEvent);

            // Then
            assertTrue(result, "Should detect high risk country: " + country);
        }
    }

    @Test
    void shouldReturnCorrectRuleName() {
        assertEquals("HIGH_RISK_COUNTRY_DETECTION", highRiskCountryDetectionService.getRuleName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = highRiskCountryDetectionService.getDescription();
        assertTrue(description.contains("high-risk countries"));
        assertTrue(description.contains("AF, IR, KP, MM, SY, YE"));
    }
}