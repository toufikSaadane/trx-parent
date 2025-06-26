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
class MT103MessageFormatterTest {

    @InjectMocks
    private MT103MessageFormatter formatter;

    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testTransaction = new Transaction(
                "12345678901234567890",
                "1234567890",
                "0987654321",
                new BigDecimal("1500.75"),
                "EUR",
                "DEUTDEFF",
                "BNPAFRPP",
                "Deutsche Bank",
                "BNP Paribas",
                LocalDateTime.of(2024, 1, 15, 14, 30),
                "PENDING"
        );
        testTransaction.setFromCountryCode("DE");
        testTransaction.setToCountryCode("FR");
        testTransaction.setFromIBAN("DE89370400440532013000");
        testTransaction.setToIBAN("FR1420041010050500013M02606");
    }

    @Test
    void testFormatToMT103_ReturnsValidFormat() {
        String mt103 = formatter.formatToMT103(testTransaction);

        assertNotNull(mt103);
        assertTrue(mt103.contains("{1:F01"));
        assertTrue(mt103.contains("{2:I103"));
        assertTrue(mt103.contains("{3:{108:"));
        assertTrue(mt103.contains("{4:"));
        assertTrue(mt103.contains("{5:"));
    }

    @Test
    void testFormatToMT103_ContainsMandatoryFields() {
        String mt103 = formatter.formatToMT103(testTransaction);

        assertTrue(mt103.contains(":20:")); // Transaction reference
        assertTrue(mt103.contains(":23B:CRED")); // Bank operation code
        assertTrue(mt103.contains(":32A:")); // Value date and amount
        assertTrue(mt103.contains("EUR1500,75")); // Currency and amount
    }

    @Test
    void testFormatToMT103_ContainsPartyFields() {
        String mt103 = formatter.formatToMT103(testTransaction);

        assertTrue(mt103.contains(":50K:")); // Ordering customer
        assertTrue(mt103.contains(":59:")); // Beneficiary customer
        assertTrue(mt103.contains("Deutsche Bank"));
        assertTrue(mt103.contains("BNP Paribas"));
    }

    @Test
    void testFormatToMT103_HandlesIBANCorrectly() {
        String mt103 = formatter.formatToMT103(testTransaction);

        assertTrue(mt103.contains("DE89370400440532013000"));
        assertTrue(mt103.contains("FR1420041010050500013M02606"));
    }
}