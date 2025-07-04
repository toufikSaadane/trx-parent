package com.toufik.trxalertservice.controller;

import com.toufik.trxalertservice.entity.TransactionEntity;
import com.toufik.trxalertservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private List<TransactionEntity> testTransactions;
    private List<TransactionEntity> fraudulentTransactions;

    @BeforeEach
    void setUp() {
        // Create test transactions
        TransactionEntity normalTransaction = createTransactionEntity("TXN001", false);
        TransactionEntity fraudTransaction = createTransactionEntity("TXN002", true);
        fraudTransaction.setFraudReasons(Arrays.asList("HIGH_RISK_COUNTRY: High risk country detected"));

        testTransactions = Arrays.asList(normalTransaction, fraudTransaction);
        fraudulentTransactions = Arrays.asList(fraudTransaction);
    }

    @Test
    void shouldGetAllTransactions() throws Exception {
        // Given
        when(transactionService.getAllTransactions()).thenReturn(testTransactions);

        // When & Then
        mockMvc.perform(get("/api/transactions/alerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value("TXN001"))
                .andExpect(jsonPath("$[0].fraudulent").value(false))
                .andExpect(jsonPath("$[1].transactionId").value("TXN002"))
                .andExpect(jsonPath("$[1].fraudulent").value(true));

        verify(transactionService).getAllTransactions();
    }

    @Test
    void shouldGetFraudulentTransactionsWithoutDateRange() throws Exception {
        // Given
        when(transactionService.getFraudulentTransactions()).thenReturn(fraudulentTransactions);

        // When & Then
        mockMvc.perform(get("/api/transactions/alerts/fraudulent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value("TXN002"))
                .andExpect(jsonPath("$[0].fraudulent").value(true))
                .andExpect(jsonPath("$[0].fraudReasons[0]").value("HIGH_RISK_COUNTRY: High risk country detected"));

        verify(transactionService).getFraudulentTransactions();
        verify(transactionService, never()).getFraudulentTransactionsBetween(any(), any());
    }

    @Test
    void shouldGetFraudulentTransactionsWithDateRange() throws Exception {
        // Given
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);
        when(transactionService.getFraudulentTransactionsBetween(start, end)).thenReturn(fraudulentTransactions);

        // When & Then
        mockMvc.perform(get("/api/transactions/alerts/fraudulent")
                        .param("start", "2024-01-01T00:00:00")
                        .param("end", "2024-01-31T23:59:00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value("TXN002"))
                .andExpect(jsonPath("$[0].fraudulent").value(true));

        verify(transactionService).getFraudulentTransactionsBetween(start, end);
        verify(transactionService, never()).getFraudulentTransactions();
    }

    @Test
    void shouldReturnEmptyListWhenNoTransactionsFound() throws Exception {
        // Given
        when(transactionService.getAllTransactions()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/transactions/alerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).getAllTransactions();
    }

    private TransactionEntity createTransactionEntity(String transactionId, boolean fraudulent) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(transactionId);
        entity.setFromAccount("ACC001");
        entity.setToAccount("ACC002");
        entity.setAmount(new BigDecimal("10000"));
        entity.setCurrency("USD");
        entity.setFromBankSwift("BANKUS33");
        entity.setToBankSwift("BANKGB22");
        entity.setFromBankName("Bank USA");
        entity.setToBankName("Bank UK");
        entity.setTimestamp(LocalDateTime.of(2024, 1, 15, 10, 0));
        entity.setStatus("COMPLETED");
        entity.setFromIBAN("US12345");
        entity.setToIBAN("GB67890");
        entity.setFromCountryCode("US");
        entity.setToCountryCode("GB");
        entity.setFraudulent(fraudulent);
        entity.setProcessedAt(LocalDateTime.now());
        return entity;
    }
}