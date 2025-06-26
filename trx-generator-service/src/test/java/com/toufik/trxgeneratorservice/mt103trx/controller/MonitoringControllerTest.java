package com.toufik.trxgeneratorservice.mt103trx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toufik.trxgeneratorservice.mt103trx.entity.TransactionEntity;
import com.toufik.trxgeneratorservice.mt103trx.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionEntity normalTransaction;
    private TransactionEntity fraudTransaction;
    private TransactionEntity invalidTransaction;
    private List<TransactionEntity> sampleTransactions;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        normalTransaction = createTransactionEntity(
                "1", "TXN001", "NORMAL", null, null, 0.1, now
        );

        fraudTransaction = createTransactionEntity(
                "2", "TXN002", "FRAUD", "UNUSUAL_AMOUNT", null, 0.9, now.minusMinutes(30)
        );

        invalidTransaction = createTransactionEntity(
                "3", "TXN003", "INVALID", null, "INVALID_ACCOUNT", 0.0, now.minusHours(1)
        );

        sampleTransactions = Arrays.asList(normalTransaction, fraudTransaction, invalidTransaction);
    }

    private TransactionEntity createTransactionEntity(String id, String transactionId,
                                                      String type, String fraudPattern,
                                                      String invalidReason, Double riskScore,
                                                      LocalDateTime timestamp) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(id);
        entity.setTransactionId(transactionId);
        entity.setFromAccount("ACC001");
        entity.setToAccount("ACC002");
        entity.setAmount(new BigDecimal("1000.00"));
        entity.setCurrency("USD");
        entity.setFromBankSwift("ABCDUS33");
        entity.setToBankSwift("EFGHGB22");
        entity.setFromBankName("Bank A");
        entity.setToBankName("Bank B");
        entity.setTimestamp(timestamp);
        entity.setStatus("COMPLETED");
        entity.setFromIBAN("US12ABCD12345678901234567890");
        entity.setToIBAN("GB33EFGH12345678901234567890");
        entity.setFromCountryCode("US");
        entity.setToCountryCode("GB");
        entity.setMt103Content("{20:FT12345678}");
        entity.setTransactionType(type);
        entity.setFraudPattern(fraudPattern);
        entity.setInvalidReason(invalidReason);
        entity.setRiskScore(riskScore);
        entity.setIsProcessed(true);
        entity.setCreatedAt(timestamp);
        entity.setUpdatedAt(timestamp);
        return entity;
    }

    @Test
    void getStats_ShouldReturnTransactionStatistics() throws Exception {
        // Given
        when(transactionRepository.count()).thenReturn(100L);
        when(transactionRepository.countByTransactionType("NORMAL")).thenReturn(70L);
        when(transactionRepository.countByTransactionType("FRAUD")).thenReturn(20L);
        when(transactionRepository.countByTransactionType("INVALID")).thenReturn(10L);

        // When & Then
        mockMvc.perform(get("/api/monitoring/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.normal").value(70))
                .andExpect(jsonPath("$.fraud").value(20))
                .andExpect(jsonPath("$.invalid").value(10));
    }

    @Test
    void getRecentTransactions_ShouldReturnLimitedRecentTransactions() throws Exception {
        // Given - Create 15 transactions but expect only 10 in response
        List<TransactionEntity> manyTransactions = Arrays.asList(
                normalTransaction, fraudTransaction, invalidTransaction,
                normalTransaction, fraudTransaction, invalidTransaction,
                normalTransaction, fraudTransaction, invalidTransaction,
                normalTransaction, fraudTransaction, invalidTransaction,
                normalTransaction, fraudTransaction, invalidTransaction
        );
        when(transactionRepository.findRecentTransactions()).thenReturn(manyTransactions);

        // When & Then
        mockMvc.perform(get("/api/monitoring/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].transactionId").value("TXN001"))
                .andExpect(jsonPath("$[0].transactionType").value("NORMAL"));
    }

    @Test
    void getRecentTransactions_WhenLessThan10Available_ShouldReturnAll() throws Exception {
        // Given
        when(transactionRepository.findRecentTransactions()).thenReturn(sampleTransactions);

        // When & Then
        mockMvc.perform(get("/api/monitoring/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].transactionId").value("TXN001"))
                .andExpect(jsonPath("$[1].transactionId").value("TXN002"))
                .andExpect(jsonPath("$[2].transactionId").value("TXN003"));
    }

    @Test
    void getFraudTransactions_ShouldReturnOnlyFraudTransactions() throws Exception {
        // Given
        when(transactionRepository.findByTransactionType("FRAUD"))
                .thenReturn(Arrays.asList(fraudTransaction));

        // When & Then
        mockMvc.perform(get("/api/monitoring/fraud")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionType").value("FRAUD"))
                .andExpect(jsonPath("$[0].fraudPattern").value("UNUSUAL_AMOUNT"))
                .andExpect(jsonPath("$[0].riskScore").value(0.9));
    }

    @Test
    void getTransactionsByType_WithValidType_ShouldReturnFilteredTransactions() throws Exception {
        // Given
        when(transactionRepository.findByTransactionType("NORMAL"))
                .thenReturn(Arrays.asList(normalTransaction));

        // When & Then
        mockMvc.perform(get("/api/monitoring/type/normal")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionType").value("NORMAL"));
    }

    @Test
    void getTransactionsByType_WithLowercaseType_ShouldConvertToUppercase() throws Exception {
        // Given
        when(transactionRepository.findByTransactionType("INVALID"))
                .thenReturn(Arrays.asList(invalidTransaction));

        // When & Then
        mockMvc.perform(get("/api/monitoring/type/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionType").value("INVALID"))
                .andExpect(jsonPath("$[0].invalidReason").value("INVALID_ACCOUNT"));
    }

    @Test
    void getTodayTransactions_ShouldReturnTransactionsFromToday() throws Exception {
        // Given
        when(transactionRepository.findByTimestampBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(sampleTransactions);

        // When & Then
        mockMvc.perform(get("/api/monitoring/today")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getAllTransactions_ShouldReturnAllTransactions() throws Exception {
        // Given
        when(transactionRepository.findAll()).thenReturn(sampleTransactions);

        // When & Then
        mockMvc.perform(get("/api/monitoring/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].transactionId",
                        containsInAnyOrder("TXN001", "TXN002", "TXN003")));
    }

    @Test
    void getStats_WhenNoTransactions_ShouldReturnZeroStats() throws Exception {
        // Given
        when(transactionRepository.count()).thenReturn(0L);
        when(transactionRepository.countByTransactionType(anyString())).thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/api/monitoring/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.normal").value(0))
                .andExpect(jsonPath("$.fraud").value(0))
                .andExpect(jsonPath("$.invalid").value(0));
    }

    @Test
    void getRecentTransactions_WhenNoTransactions_ShouldReturnEmptyList() throws Exception {
        // Given
        when(transactionRepository.findRecentTransactions()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/monitoring/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getFraudTransactions_WhenNoFraudTransactions_ShouldReturnEmptyList() throws Exception {
        // Given
        when(transactionRepository.findByTransactionType("FRAUD")).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/monitoring/fraud")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTransactionsByType_WithNonExistentType_ShouldReturnEmptyList() throws Exception {
        // Given
        when(transactionRepository.findByTransactionType("NONEXISTENT")).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/monitoring/type/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void corsConfiguration_ShouldAllowCrossOriginRequests() throws Exception {
        // Given
        when(transactionRepository.count()).thenReturn(1L);
        when(transactionRepository.countByTransactionType(anyString())).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/api/monitoring/stats")
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}