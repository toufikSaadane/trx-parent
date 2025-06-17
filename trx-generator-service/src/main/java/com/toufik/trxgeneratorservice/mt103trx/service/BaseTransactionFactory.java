package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
public abstract class BaseTransactionFactory {

    @Autowired
    protected BankDataService bankDataService;

    protected final Random random = new Random();
    protected final String[] transactionStatuses = {"PENDING", "PROCESSING"};

    /**
     * Creates a base transaction with standard generation logic
     */
    protected Transaction createBaseTransaction() {
        BankInfo fromBank = bankDataService.getRandomBank();
        BankInfo toBank = getDistinctToBank(fromBank);

        String fromAccount = generateAccountNumber();
        String toAccount = generateAccountNumber();
        String fromIBAN = generateIBANForBank(fromBank, fromAccount);
        String toIBAN = generateIBANForBank(toBank, toAccount);

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

        transaction.setFromIBAN(fromIBAN);
        transaction.setToIBAN(toIBAN);
        transaction.setFromCountryCode(fromBank.getCountryCode());
        transaction.setToCountryCode(toBank.getCountryCode());

        return transaction;
    }

    protected BankInfo getDistinctToBank(BankInfo fromBank) {
        BankInfo toBank = bankDataService.getRandomBank();
        int attempts = 0;

        while (fromBank.getSwiftCode().equals(toBank.getSwiftCode()) && attempts < 10) {
            toBank = bankDataService.getRandomBank();
            attempts++;
        }

        return toBank;
    }

    protected String generateIBANForBank(BankInfo bank, String accountNumber) {
        try {
            String iban = bankDataService.generateIBAN(bank, accountNumber);
            if (iban != null) {
                log.debug("Generated IBAN {} for bank {}", iban, bank.getSwiftCode());
                return iban;
            } else {
                log.warn("Could not generate IBAN for bank {} ({}), country: {}",
                        bank.getBankName(), bank.getSwiftCode(), bank.getCountryCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error generating IBAN for bank {}: {}", bank.getSwiftCode(), e.getMessage());
            return null;
        }
    }

    protected String determineCurrency(BankInfo fromBank, BankInfo toBank) {
        if (isEuropeanBank(fromBank) && isEuropeanBank(toBank)) {
            return "EUR";
        } else if (isUSBank(fromBank) || isUSBank(toBank)) {
            return "USD";
        }
        return fromBank.getCurrencyCode();
    }

    protected boolean isEuropeanBank(BankInfo bank) {
        return bank.getCountryCode().matches("DE|FR|IT|ES|NL|BE|AT|PT|IE|FI|LU");
    }

    protected boolean isUSBank(BankInfo bank) {
        return "US".equals(bank.getCountryCode());
    }

    protected String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();
        int length = random.nextDouble() < 0.7 ? 12 : 10;

        for (int i = 0; i < length; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    /**
     * Abstract method to be implemented by subclasses for different amount generation strategies
     */
    protected abstract BigDecimal generateRandomAmount();
}