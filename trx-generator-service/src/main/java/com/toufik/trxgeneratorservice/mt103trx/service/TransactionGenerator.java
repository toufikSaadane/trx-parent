package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.service.BankDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Generates random transactions for testing purposes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionGenerator {

    private final BankDataService bankDataService;
    private final Random random = new Random();
    private final String[] transactionStatuses = {"PENDING", "COMPLETED", "FAILED", "PROCESSING"};

    /**
     * Generates a random transaction with realistic data
     */
    public Transaction generateRandomTransaction() {
        BankInfo fromBank = bankDataService.getRandomBank();
        BankInfo toBank = getDistinctToBank(fromBank);

        String fromAccount = generateAccountNumber();
        String toAccount = generateAccountNumber();

        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                fromAccount,
                toAccount,
                generateRandomAmount(),
                determineCurrency(fromBank, toBank),
                fromBank.getSwiftCode(),
                toBank.getSwiftCode(),
                fromBank.getBankName(),
                toBank.getBankName(),
                LocalDateTime.now(),
                transactionStatuses[random.nextInt(transactionStatuses.length)]
        );

        transaction.setFromCountryCode(fromBank.getCountryCode());
        transaction.setToCountryCode(toBank.getCountryCode());

        return transaction;
    }

    private BankInfo getDistinctToBank(BankInfo fromBank) {
        BankInfo toBank = bankDataService.getRandomBank();
        int attempts = 0;

        while (fromBank.getSwiftCode().equals(toBank.getSwiftCode()) && attempts < 10) {
            toBank = bankDataService.getRandomBank();
            attempts++;
        }

        return toBank;
    }

    private String determineCurrency(BankInfo fromBank, BankInfo toBank) {
        String currency = fromBank.getCurrencyCode();

        if (isEuropeanBank(fromBank) && isEuropeanBank(toBank)) {
            return "EUR";
        } else if (isUSBank(fromBank) || isUSBank(toBank)) {
            return "USD";
        }

        return currency;
    }

    private boolean isEuropeanBank(BankInfo bank) {
        return bank.getCountryCode().matches("DE|FR|IT|ES|NL|BE|AT|PT|IE|FI|LU");
    }

    private boolean isUSBank(BankInfo bank) {
        return "US".equals(bank.getCountryCode());
    }

    private String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();
        int length = random.nextDouble() < 0.7 ? 12 : 10;

        for (int i = 0; i < length; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    private BigDecimal generateRandomAmount() {
        double randomValue = random.nextDouble();
        double amount;

        if (randomValue < 0.5) {
            amount = 10.0 + (random.nextDouble() * 990.0);
        } else if (randomValue < 0.8) {
            amount = 1000.0 + (random.nextDouble() * 9000.0);
        } else {
            amount = 10000.0 + (random.nextDouble() * 90000.0);
        }

        return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
    }
}