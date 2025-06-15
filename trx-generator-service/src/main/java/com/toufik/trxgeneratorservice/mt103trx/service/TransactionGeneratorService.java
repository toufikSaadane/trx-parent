package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import com.toufik.trxgeneratorservice.mt103trx.model.TransactionWithMT103Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class TransactionGeneratorService {

    private final BankDataService bankDataService;

    private final TransactionProducer transactionProducer;


    private final MT103MessageFormatter mt103MessageFormatter;

    public TransactionGeneratorService(BankDataService bankDataService, TransactionProducer transactionProducer, @Qualifier("MT103MessageFormatter") MT103MessageFormatter mt103MessageFormatter) {
        this.bankDataService = bankDataService;
        this.transactionProducer = transactionProducer;
        this.mt103MessageFormatter = mt103MessageFormatter;
    }

    private final Random random = new Random();
    private final String[] transactionStatuses = {"PENDING", "COMPLETED", "FAILED", "PROCESSING"};

    @Scheduled(fixedRate = 10000)
    public void generateAndSendTransaction() {
        try {
            TransactionWithMT103Event transactionWithMT103Event = generateRandomTransactionWithMT103();
            log.info("Generated transaction: {}", transactionWithMT103Event.getTransaction().getTransactionId());
            log.info("======================= TRANSACTION =============================");
            log.info("Transaction Details: {}", transactionWithMT103Event.getTransaction());
            log.info("From IBAN: {}", transactionWithMT103Event.getTransaction().getFromIBAN());
            log.info("To IBAN: {}", transactionWithMT103Event.getTransaction().getToIBAN());
            transactionProducer.sendTransaction(transactionWithMT103Event);
        } catch (Exception e) {
            log.error("Error generating transaction: {}", e.getMessage(), e);
        }
    }

    private TransactionWithMT103Event generateRandomTransactionWithMT103() {
        Transaction transaction = generateRandomTransaction();
        String mt103Content = mt103MessageFormatter.formatToMT103(transaction);

        TransactionWithMT103Event result = new TransactionWithMT103Event();
        result.setTransaction(transaction);
        result.setMt103Content(mt103Content);

        return result;
    }

    public Transaction generateRandomTransaction() {
        BankInfo fromBank = bankDataService.getRandomBank();
        BankInfo toBank = getDistinctToBank(fromBank);

        // Generate account numbers
        String fromAccount = generateAccountNumber();
        String toAccount = generateAccountNumber();

        // Generate IBANs
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

        // Set IBAN fields
        transaction.setFromIBAN(fromIBAN);
        log.info("Generated From IBAN: {}", fromIBAN);
        transaction.setToIBAN(toIBAN);
        transaction.setFromCountryCode(fromBank.getCountryCode());
        transaction.setToCountryCode(toBank.getCountryCode());

        return transaction;
    }

    private BankInfo getDistinctToBank(BankInfo fromBank) {
        BankInfo toBank = bankDataService.getRandomBank();
        int attempts = 0;

        // Try to get a different bank, but don't loop forever
        while (fromBank.getSwiftCode().equals(toBank.getSwiftCode()) && attempts < 10) {
            toBank = bankDataService.getRandomBank();
            attempts++;
        }

        return toBank;
    }

    private String generateIBANForBank(BankInfo bank, String accountNumber) {
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

    private String determineCurrency(BankInfo fromBank, BankInfo toBank) {
        // Use sender's currency as primary, but could implement more sophisticated logic
        String currency = fromBank.getCurrencyCode();

        // For certain country pairs, prefer major currencies
        if (isEuropeanBank(fromBank) && isEuropeanBank(toBank)) {
            return "EUR";
        } else if (isUSBank(fromBank) || isUSBank(toBank)) {
            // For US transactions, prefer USD
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
        // Generate account number with different patterns for variety
        StringBuilder accountNumber = new StringBuilder();

        // 70% chance for 12 digits, 30% chance for 10 digits
        int length = random.nextDouble() < 0.7 ? 12 : 10;

        for (int i = 0; i < length; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    private BigDecimal generateRandomAmount() {
        // Generate more realistic amounts with different ranges
        double randomValue = random.nextDouble();
        double amount;

        if (randomValue < 0.5) {
            // 50% - Small amounts: 10-1000
            amount = 10.0 + (random.nextDouble() * 990.0);
        } else if (randomValue < 0.8) {
            // 30% - Medium amounts: 1000-10000
            amount = 1000.0 + (random.nextDouble() * 9000.0);
        } else {
            // 20% - Large amounts: 10000-100000
            amount = 10000.0 + (random.nextDouble() * 90000.0);
        }

        return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
    }
}