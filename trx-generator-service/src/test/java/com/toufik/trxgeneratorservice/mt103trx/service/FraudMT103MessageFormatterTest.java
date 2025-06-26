package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FraudMT103MessageFormatterTest {

    @InjectMocks
    private FraudMT103MessageFormatter formatter;

    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testTransaction = new Transaction(
                "12345678901234567890",
                "1234567890",
                "0987654321",
                new BigDecimal("20000.00"),
                "USD",
                "DEUTDEFF",
                "CHASUS33",
                "Deutsche Bank",
                "JPMorgan Chase",
                LocalDateTime.of(2024, 1, 15, 3, 30), // Off-hours time
                "PENDING"
        );
        testTransaction.setFromCountryCode("DE");
        testTransaction.setToCountryCode("US");
        testTransaction.setFromIBAN("DE89370400440532013000");
        testTransaction.setToIBAN("This country does not use IBAN");
    }

    @Test
    void testFormatToMT103_ReturnsValidMT103() {
        String mt103 = formatter.formatToMT103(testTransaction);

        assertNotNull(mt103);
        assertTrue(mt103.contains("{1:F01"));
        assertTrue(mt103.contains("{2:I103"));
        assertTrue(mt103.contains("{4:"));
        assertTrue(mt103.contains(":20:"));
        assertTrue(mt103.contains(":32A:"));
    }

    @Test
    void testFormatToMT103_HighAmountTransaction() {
        testTransaction.setAmount(new BigDecimal("25000.00"));

        String mt103 = formatter.formatToMT103(testTransaction);

        assertTrue(mt103.contains("Large value transfer"));
    }

    @Test
    void testFormatToMT103_RoundAmountTransaction() {
        testTransaction.setAmount(new BigDecimal("5000.00"));

        String mt103 = formatter.formatToMT103(testTransaction);

        assertTrue(mt103.contains("Business payment"));
    }

    @Test
    void testFormatToMT103_CrossBorderTransaction() {
        String mt103 = formatter.formatToMT103(testTransaction);

        // Cross-border should have SHA charge bearer
        assertTrue(mt103.contains(":71A:SHA"));
    }
}