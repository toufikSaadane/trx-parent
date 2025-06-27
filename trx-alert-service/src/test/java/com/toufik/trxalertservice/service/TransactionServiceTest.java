package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.entity.TransactionEntity;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.model.Transaction;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import com.toufik.trxalertservice.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AlertRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionWithMT103Event testEvent;
    private TransactionEntity savedEntity;

    @BeforeEach
    void setUp() {
        Transaction transaction = Transaction.builder()
                .transactionId("TXN001")
                .fromAccount("ACC001")
                .toAccount("ACC002")
                .amount(new BigDecimal("50000"))
                .currency("USD")
                .fromBankSwift("BANKUS33")
                .toBankSwift("BANKGB22")
                .fromBankName("Bank USA")
                .toBankName("Bank UK")
                .timestamp(LocalDateTime.of(2024, 1, 1, 10, 0))
                .status("COMPLETED")
                .fromIBAN("US12345")
                .toIBAN("GB67890")
                .fromCountryCode("US")
                .toCountryCode("GB")
                .build();

        testEvent = new TransactionWithMT103Event(transaction, "MT103 content");

        savedEntity = new TransactionEntity();
        savedEntity.setTransactionId("TXN001");
        savedEntity.setFraudulent(false);
    }

    @Test
    void shouldSaveTransactionWithoutFraudAlerts() {

        List<FraudAlert> emptyFraudAlerts = Arrays.asList();
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
        TransactionEntity result = transactionService.saveTransaction(testEvent, emptyFraudAlerts);
        verify(transactionRepository).save(argThat(entity -> {
            return entity.getTransactionId().equals("TXN001") &&
                    entity.getAmount().equals(new BigDecimal("50000")) &&
                    entity.getCurrency().equals("USD") &&
                    !entity.isFraudulent() &&
                    entity.getFraudReasons().isEmpty() &&
                    entity.getMt103Content().equals("MT103 content");
        }));

        assertNotNull(result);
        assertEquals("TXN001", result.getTransactionId());
    }

    @Test
    void shouldSaveTransactionWithFraudAlerts() {
        // Given
        List<FraudAlert> fraudAlerts = Arrays.asList(
                new FraudAlert("TXN001", "HIGH_RISK_COUNTRY", "High risk country detected", "HIGH", LocalDateTime.now(), "Details1"),
                new FraudAlert("TXN001", "HIGH_AMOUNT", "High amount detected", "HIGH", LocalDateTime.now(), "Details2")
        );

        savedEntity.setFraudulent(true);
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedEntity);

        // When
        TransactionEntity result = transactionService.saveTransaction(testEvent, fraudAlerts);

        // Then
        verify(transactionRepository).save(argThat(entity -> {
            return entity.getTransactionId().equals("TXN001") &&
                    entity.isFraudulent() &&
                    entity.getFraudReasons().size() == 2 &&
                    entity.getFraudReasons().contains("HIGH_RISK_COUNTRY: High risk country detected") &&
                    entity.getFraudReasons().contains("HIGH_AMOUNT: High amount detected");
        }));

        assertNotNull(result);
        assertTrue(result.isFraudulent());
    }

    @Test
    void shouldRetrieveFraudulentTransactions() {
        // Given
        List<TransactionEntity> expectedFraudulentTransactions = Arrays.asList(
                createFraudulentEntity("TXN001"),
                createFraudulentEntity("TXN002")
        );
        when(transactionRepository.findByFraudulent(true)).thenReturn(expectedFraudulentTransactions);

        // When
        List<TransactionEntity> result = transactionService.getFraudulentTransactions();

        // Then
        verify(transactionRepository).findByFraudulent(true);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(TransactionEntity::isFraudulent));
    }

    private TransactionEntity createFraudulentEntity(String transactionId) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(transactionId);
        entity.setFraudulent(true);
        entity.setFraudReasons(Arrays.asList("HIGH_RISK_COUNTRY: High risk country detected"));
        return entity;
    }
}