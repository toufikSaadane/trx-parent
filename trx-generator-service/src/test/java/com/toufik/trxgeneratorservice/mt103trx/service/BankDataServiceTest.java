// Language: Java
package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BankDataServiceTest {

    private BankDataService bankDataService;

    @BeforeEach
    void setUp() {
        bankDataService = new BankDataService();
        // Clear out banks list and override randomness if needed.
        ReflectionTestUtils.setField(bankDataService, "banks", new ArrayList<>());
        ReflectionTestUtils.setField(bankDataService, "random", new Random());
    }

    @Test
    void loadBanksFromCsv_ShouldLoadValidBanks() {
        bankDataService.loadBanksFromCsv();
        @SuppressWarnings("unchecked")
        List<BankInfo> banks = (List<BankInfo>) ReflectionTestUtils.getField(bankDataService, "banks");
        assertEquals(5, banks.size());

        BankInfo deutscheBank = banks.get(0);
        assertEquals("DEUTDEFF", deutscheBank.getSwiftCode());
        assertEquals("DE", deutscheBank.getCountryCode());
        assertEquals("Deutsche Bank", deutscheBank.getBankName());
        assertEquals("12345678", deutscheBank.getRoutingNumber());
        assertEquals("EUR", deutscheBank.getCurrencyCode());
        assertEquals("DE", deutscheBank.getIbanPrefix());
        assertEquals(22, deutscheBank.getIbanLength());

        BankInfo chaseBank = banks.get(1);
        assertEquals("CHASUS33", chaseBank.getSwiftCode());
        assertEquals("US", chaseBank.getCountryCode());
        assertEquals("Chase Bank", chaseBank.getBankName());
        assertEquals("021000021", chaseBank.getRoutingNumber());
        assertEquals("USD", chaseBank.getCurrencyCode());
        // When IbanPrefix is missing, the country code should be used.
        assertEquals("US", chaseBank.getIbanPrefix());
        assertEquals(0, chaseBank.getIbanLength());
    }

    @Test
    void getRandomBank_ShouldReturnRandomBank() {
        List<BankInfo> banks = createSampleBanks();
        ReflectionTestUtils.setField(bankDataService, "banks", banks);
        // Set a fixed random value by replacing the Random instance.
        Random fixedRandom = new Random() {
            @Override
            public int nextInt(int bound) {
                return 1; // always return the bank at index 1
            }
        };
        ReflectionTestUtils.setField(bankDataService, "random", fixedRandom);
        BankInfo result = bankDataService.getRandomBank();
        assertEquals("BARCGB22", result.getSwiftCode());
        assertEquals("Barclays", result.getBankName());
    }

    @Test
    void generateIBAN_ShouldReturnMessageForNonIBANCountry() {
        BankInfo usBank = new BankInfo("CHASUS33", "US", "Chase", "021000021", "USD", "US", 0);
        String result = bankDataService.generateIBAN(usBank, "123456789");
        assertEquals("This country does not use IBAN", result);
    }

    @Test
    void generateIBAN_ShouldReturnMessageForNullIbanLength() {
        BankInfo bankWithNullLength = new BankInfo("TESTBANK", "XX", "Test Bank", "12345", "USD", "XX", null);
        String result = bankDataService.generateIBAN(bankWithNullLength, "123456789");
        assertEquals("This country does not use IBAN", result);
    }

    @Test
    void generateIBAN_ShouldGenerateValidIBANForGermanBank() {
        BankInfo germanBank = new BankInfo("DEUTDEFF", "DE", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        String result = bankDataService.generateIBAN(germanBank, "123456789");
        assertTrue(result.startsWith("DE"));
        assertEquals(22, result.length());
        assertTrue(result.matches("DE\\d{20}"));
        String bankCode = result.substring(4, 12);
        assertEquals("12345678", bankCode);
    }

    @Test
    void generateIBAN_ShouldGenerateValidIBANForFrenchBank() {
        BankInfo frenchBank = new BankInfo("BNPAFRPP", "FR", "BNP Paribas", "30004", "EUR", "FR", 27);
        String result = bankDataService.generateIBAN(frenchBank, "987654321");
        assertTrue(result.startsWith("FR"));
        assertEquals(27, result.length());
        assertTrue(result.matches("FR\\d{25}"));
    }

    @Test
    void generateIBAN_ShouldTruncateLongBankCode() {
        BankInfo bankWithLongCode = new BankInfo("TESTBANK", "DE", "Test Bank", "123456789012345", "EUR", "DE", 22);
        String result = bankDataService.generateIBAN(bankWithLongCode, "123456789");
        assertTrue(result.startsWith("DE"));
        assertEquals(22, result.length());
        String bankCode = result.substring(4, 12);
        assertEquals("12345678", bankCode);
    }

    @Test
    void generateIBAN_ShouldHandleNonNumericCharactersInRoutingNumber() {
        BankInfo bankWithAlphaRouting = new BankInfo("TESTBANK", "DE", "Test Bank", "ABC123DEF456", "EUR", "DE", 22);
        String result = bankDataService.generateIBAN(bankWithAlphaRouting, "123456789");
        assertTrue(result.startsWith("DE"));
        assertEquals(22, result.length());
        String bankCode = result.substring(4, 12);
        // From "ABC123DEF456", only digits "123" and "456" are picked.
        // The service extracts digits and truncates to 8 characters.
        assertEquals("12345600", bankCode);
    }

    @Test
    void generateIBAN_CheckDigitValidation() {
        BankInfo germanBank = new BankInfo("DEUTDEFF", "DE", "Deutsche Bank", "12345678", "EUR", "DE", 22);
        String iban = bankDataService.generateIBAN(germanBank, "123456789");
        String checkDigits = iban.substring(2, 4);

        String bankCode = iban.substring(4, 12);
        String account = iban.substring(12);
        String temp = bankCode + account + "DE00";
        StringBuilder numeric = new StringBuilder();
        for (char c : temp.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(c - 'A' + 10);
            } else {
                numeric.append(c);
            }
        }
        int remainder = 0;
        for (char digit : numeric.toString().toCharArray()) {
            remainder = (remainder * 10 + Character.getNumericValue(digit)) % 97;
        }
        int expectedCheck = 98 - remainder;
        assertEquals(String.format("%02d", expectedCheck), checkDigits);
    }

    private List<BankInfo> createSampleBanks() {
        List<BankInfo> banks = new ArrayList<>();
        banks.add(new BankInfo("DEUTDEFF", "DE", "Deutsche Bank", "12345678", "EUR", "DE", 22));
        banks.add(new BankInfo("BARCGB22", "GB", "Barclays", "203002", "GBP", "GB", 22));
        banks.add(new BankInfo("CHASUS33", "US", "Chase", "021000021", "USD", "US", 0));
        return banks;
    }
}