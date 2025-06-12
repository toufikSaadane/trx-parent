package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MT103MessageFormatterTest {

    private MT103MessageFormatter formatter;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        formatter = new MT103MessageFormatter();

        transaction = new Transaction();
        transaction.setTransactionId("TXN123456789");
        transaction.setFromBankSwift("CHASUS33");
        transaction.setToBankSwift("DEUTDEFF");
        transaction.setFromBankName("Chase Bank");
        transaction.setToBankName("Deutsche Bank");
        transaction.setFromAccount("123456789");
        transaction.setToAccount("987654321");
        transaction.setFromIBAN("US64CHASUS33123456789");
        transaction.setToIBAN("DE89370400440532013000");
        transaction.setAmount(new BigDecimal("1500.50"));
        transaction.setCurrency("USD");
        transaction.setTimestamp(LocalDateTime.of(2024, 6, 15, 10, 30));
        transaction.setFromCountryCode("US");
        transaction.setToCountryCode("DE");
    }

    @Test
    void formatToMT103_shouldContainBasicStructure() {
        String result = formatter.formatToMT103(transaction);

        assertNotNull(result);
        assertTrue(result.contains("{1:F01"));
        assertTrue(result.contains("{2:I103"));
        assertTrue(result.contains("{3:{108:"));
        assertTrue(result.contains("{4:"));
        assertTrue(result.contains("{5:{MAC:"));
    }

    @Test
    void formatToMT103_shouldContainMandatoryFields() {
        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains(":20:TXN123456789"));
        assertTrue(result.contains(":23B:CRED"));
        assertTrue(result.contains(":32A:240615USD1500,50"));
        assertTrue(result.contains(":33B:USD1500,50"));
    }

    @Test
    void formatToMT103_shouldContainPartyInformation() {
        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains(":50K:/US64CHASUS33123456789"));
        assertTrue(result.contains("Chase Bank"));
        assertTrue(result.contains(":52A:CHASUS33"));
        assertTrue(result.contains(":57A:DEUTDEFF"));
        assertTrue(result.contains(":59:/DE89370400440532013000"));
        assertTrue(result.contains("Deutsche Bank"));
    }

    @Test
    void formatToMT103_shouldHandleCrossBorderTransaction() {
        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains(":71A:SHA"));
        assertTrue(result.contains(":56A:DEUTDEFFXXX"));
        assertTrue(result.contains("Cross-border transfer"));
    }

    @Test
    void formatToMT103_shouldHandleDomesticTransaction() {
        transaction.setToBankSwift("CHASUS44");
        transaction.setToCountryCode("US");

        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains(":71A:OUR"));
        assertFalse(result.contains(":56A:"));
    }

    @Test
    void formatToMT103_shouldHandleNullIBAN() {
        transaction.setFromIBAN(null);
        transaction.setToIBAN("This country does not use IBAN");

        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains(":50K:/123456789"));
        assertTrue(result.contains(":59:/987654321"));
    }

    @Test
    void formatToMT103_shouldTruncateTransactionId() {
        transaction.setTransactionId("VERY_LONG_TRANSACTION_ID_THAT_EXCEEDS_LIMIT");

        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains(":20:VERY_LONG_TRANSA"));
        assertTrue(result.contains("{3:{108:VERY_LONG_TRANSA}"));
    }

    @Test
    void formatToMT103_shouldFormatAmountWithComma() {
        transaction.setAmount(new BigDecimal("12345.67"));

        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains("12345,67"));
        assertFalse(result.contains("12345.67"));
    }

    @Test
    void formatToMT103_shouldFormatLTAddress() {
        transaction.setFromBankSwift("CHASUS33");
        transaction.setToBankSwift("DEUTDEFFXXX");

        String result = formatter.formatToMT103(transaction);

        assertTrue(result.contains("CHASUS33XXX0"));
        assertTrue(result.contains("DEUTDEFFXXX0"));
    }
}