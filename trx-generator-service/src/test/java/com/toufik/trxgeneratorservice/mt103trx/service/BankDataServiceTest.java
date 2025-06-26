package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BankDataServiceTest {

    @InjectMocks
    private BankDataService bankDataService;

    @BeforeEach
    void setUp() {
        // Mock some banks data
        List<BankInfo> mockBanks = new ArrayList<>();
        mockBanks.add(new BankInfo("DEUTDEFF", "DE", "Germany", "Deutsche Bank", "12345678", "EUR", "DE", 22));
        mockBanks.add(new BankInfo("CHASUS33", "US", "United States", "JPMorgan Chase", "87654321", "USD", "US", null));

        ReflectionTestUtils.setField(bankDataService, "banks", mockBanks);
    }

    @Test
    void testGetRandomBank_ReturnsValidBank() {
        BankInfo bank = bankDataService.getRandomBank();

        assertNotNull(bank);
        assertTrue(bank.getSwiftCode().equals("DEUTDEFF") || bank.getSwiftCode().equals("CHASUS33"));
    }

    @Test
    void testGenerateIBAN_WithValidBank() {
        BankInfo bank = new BankInfo("DEUTDEFF", "DE", "Germany", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        String accountNumber = "1234567890";

        String iban = bankDataService.generateIBAN(bank, accountNumber);

        assertNotNull(iban);
        assertTrue(iban.startsWith("DE"));
        assertEquals(22, iban.length());
    }

    @Test
    void testGenerateIBAN_WithNonIBANCountry() {
        BankInfo bank = new BankInfo("CHASUS33", "US", "United States", "JPMorgan Chase", "87654321", "USD", "US", null);
        String accountNumber = "1234567890";

        String iban = bankDataService.generateIBAN(bank, accountNumber);

        assertEquals("This country does not use IBAN", iban);
    }

    @Test
    void testGenerateIBAN_WithZeroLength() {
        BankInfo bank = new BankInfo("TESTXX99", "XX", "Test Country", "Test Bank", "12345678", "XXX", "XX", 0);
        String accountNumber = "1234567890";

        String iban = bankDataService.generateIBAN(bank, accountNumber);

        assertEquals("This country does not use IBAN", iban);
    }
}