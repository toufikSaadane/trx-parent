package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseTransactionFactoryTest {

    @Mock
    private BankDataService bankDataService;

    private TestableBaseTransactionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TestableBaseTransactionFactory();
        factory.bankDataService = bankDataService;
    }

    @Test
    void testCreateBaseTransaction_ReturnsValidTransaction() {
        BankInfo fromBank = new BankInfo("DEUTDEFF", "DE", "Germany", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        BankInfo toBank = new BankInfo("CHASUS33", "US", "United States", "JPMorgan Chase", "87654321", "USD", "US", null);

        when(bankDataService.getRandomBank()).thenReturn(fromBank, toBank);
        when(bankDataService.generateIBAN(any(), any())).thenReturn("DE89370400440532013000", "This country does not use IBAN");

        Transaction transaction = factory.createBaseTransaction();

        assertNotNull(transaction);
        assertNotNull(transaction.getTransactionId());
        assertNotNull(transaction.getFromAccount());
        assertNotNull(transaction.getToAccount());
        assertNotNull(transaction.getAmount());
        assertEquals("USD", transaction.getCurrency()); // Should be USD when US bank is involved
    }

    @Test
    void testGetDistinctToBank_ReturnsDifferentBank() {
        BankInfo fromBank = new BankInfo("DEUTDEFF", "DE", "Germany", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        BankInfo toBank = new BankInfo("CHASUS33", "US", "United States", "JPMorgan Chase", "87654321", "USD", "US", null);

        when(bankDataService.getRandomBank()).thenReturn(toBank);

        BankInfo result = factory.getDistinctToBank(fromBank);

        assertNotNull(result);
        assertNotEquals(fromBank.getSwiftCode(), result.getSwiftCode());
    }

    @Test
    void testDetermineCurrency_EuropeanBanks() {
        BankInfo fromBank = new BankInfo("DEUTDEFF", "DE", "Germany", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        BankInfo toBank = new BankInfo("BNPAFRPP", "FR", "France", "BNP Paribas", "30004000", "EUR", "FR", 27);

        String currency = factory.determineCurrency(fromBank, toBank);

        assertEquals("EUR", currency);
    }

    @Test
    void testGenerateAccountNumber_ReturnsValidFormat() {
        String accountNumber = factory.generateAccountNumber();

        assertNotNull(accountNumber);
        assertTrue(accountNumber.length() == 10 || accountNumber.length() == 12);
        assertTrue(accountNumber.matches("\\d+"));
    }

    // Helper class to test abstract BaseTransactionFactory
    private static class TestableBaseTransactionFactory extends BaseTransactionFactory {
        @Override
        protected BigDecimal generateRandomAmount() {
            return new BigDecimal("1000.00");
        }
    }
}