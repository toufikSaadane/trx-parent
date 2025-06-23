package com.toufik.trxalertservice.fraud.service;

import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionEngineTest {

    @Mock
    private HighRiskCountryDetectionService highRiskCountryDetectionService;
    @Mock
    private HighAmountDetectionService highAmountDetectionService;
    @Mock
    private SuspiciousRemittanceDetectionService suspiciousRemittanceDetectionService;
    @Mock
    private OffHoursDetectionService offHoursDetectionService;

    @InjectMocks
    private FraudDetectionEngine fraudDetectionEngine;

    private TransactionWithMT103Event mockEvent;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockEvent = mock(TransactionWithMT103Event.class);
        mockTransaction = mock(Transaction.class);
        when(mockEvent.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.getTransactionId()).thenReturn("TXN123");
        when(mockTransaction.getAmount()).thenReturn(new BigDecimal("1000"));
        when(mockTransaction.getCurrency()).thenReturn("EUR");
        when(mockTransaction.getFromCountryCode()).thenReturn("US");
        when(mockTransaction.getToCountryCode()).thenReturn("GB");
    }

    @Test
    void shouldReturnNoAlertsWhenNoRulesTriggered() {
        // Given
        when(highRiskCountryDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(highAmountDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(suspiciousRemittanceDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(offHoursDetectionService.isSuspicious(mockEvent)).thenReturn(false);

        // When
        List<FraudAlert> alerts = fraudDetectionEngine.detectFraud(mockEvent);

        // Then
        assertTrue(alerts.isEmpty());
    }

    @Test
    void shouldReturnOneAlertWhenOneRuleTriggered() {
        // Given
        when(highRiskCountryDetectionService.isSuspicious(mockEvent)).thenReturn(true);
        when(highRiskCountryDetectionService.getRuleName()).thenReturn("HIGH_RISK_COUNTRY_DETECTION");
        when(highRiskCountryDetectionService.getDescription()).thenReturn("High risk country detected");

        when(highAmountDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(suspiciousRemittanceDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(offHoursDetectionService.isSuspicious(mockEvent)).thenReturn(false);

        // When
        List<FraudAlert> alerts = fraudDetectionEngine.detectFraud(mockEvent);

        // Then
        assertEquals(1, alerts.size());
        assertEquals("TXN123", alerts.get(0).getTransactionId());
        assertEquals("HIGH_RISK_COUNTRY_DETECTION", alerts.get(0).getRuleName());
        assertEquals("HIGH", alerts.get(0).getSeverity());
    }

    @Test
    void shouldReturnMultipleAlertsWhenMultipleRulesTriggered() {
        // Given
        when(highRiskCountryDetectionService.isSuspicious(mockEvent)).thenReturn(true);
        when(highRiskCountryDetectionService.getRuleName()).thenReturn("HIGH_RISK_COUNTRY_DETECTION");
        when(highRiskCountryDetectionService.getDescription()).thenReturn("High risk country detected");

        when(highAmountDetectionService.isSuspicious(mockEvent)).thenReturn(true);
        when(highAmountDetectionService.getRuleName()).thenReturn("HIGH_AMOUNT_DETECTION");
        when(highAmountDetectionService.getDescription()).thenReturn("High amount detected");

        when(suspiciousRemittanceDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(offHoursDetectionService.isSuspicious(mockEvent)).thenReturn(false);

        // When
        List<FraudAlert> alerts = fraudDetectionEngine.detectFraud(mockEvent);

        // Then
        assertEquals(2, alerts.size());
    }

    @Test
    void shouldAssignCorrectSeverityLevels() {

        when(offHoursDetectionService.isSuspicious(mockEvent)).thenReturn(true);
        when(offHoursDetectionService.getRuleName()).thenReturn("OFF_HOURS_DETECTION");
        when(offHoursDetectionService.getDescription()).thenReturn("Off hours detected");
        when(suspiciousRemittanceDetectionService.isSuspicious(mockEvent)).thenReturn(true);
        when(suspiciousRemittanceDetectionService.getRuleName()).thenReturn("SUSPICIOUS_REMITTANCE_DETECTION");
        when(suspiciousRemittanceDetectionService.getDescription()).thenReturn("Suspicious remittance detected");
        when(highRiskCountryDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(highAmountDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        List<FraudAlert> alerts = fraudDetectionEngine.detectFraud(mockEvent);
        assertEquals(2, alerts.size());
        FraudAlert offHoursAlert = alerts.stream()
                .filter(alert -> "OFF_HOURS_DETECTION".equals(alert.getRuleName()))
                .findFirst().orElse(null);
        assertNotNull(offHoursAlert);
        assertEquals("LOW", offHoursAlert.getSeverity());
        FraudAlert remittanceAlert = alerts.stream()
                .filter(alert -> "SUSPICIOUS_REMITTANCE_DETECTION".equals(alert.getRuleName()))
                .findFirst().orElse(null);
        assertNotNull(remittanceAlert);
        assertEquals("MEDIUM", remittanceAlert.getSeverity());
    }

    @Test
    void shouldExecuteAllRules() {
        when(highRiskCountryDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(highAmountDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(suspiciousRemittanceDetectionService.isSuspicious(mockEvent)).thenReturn(false);
        when(offHoursDetectionService.isSuspicious(mockEvent)).thenReturn(false);

        fraudDetectionEngine.detectFraud(mockEvent);

        verify(highRiskCountryDetectionService).isSuspicious(mockEvent);
        verify(highAmountDetectionService).isSuspicious(mockEvent);
        verify(suspiciousRemittanceDetectionService).isSuspicious(mockEvent);
        verify(offHoursDetectionService).isSuspicious(mockEvent);
    }
}