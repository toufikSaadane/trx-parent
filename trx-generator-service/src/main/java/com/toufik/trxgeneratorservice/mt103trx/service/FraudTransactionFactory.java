package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;

@Component
@Slf4j
public class FraudTransactionFactory extends BaseTransactionFactory {

    private static final String[] HIGH_RISK_COUNTRIES = {
            "AF", "IR", "KP", "MM", "SY", "YE"};

    private final Random random = new Random();

    public Transaction createFraudTransaction() {
        Transaction transaction = createBaseTransaction();
        applyRandomFraudPattern(transaction);
        return transaction;
    }

    private void applyRandomFraudPattern(Transaction transaction) {
        int pattern = random.nextInt(4);
        switch (pattern) {
            case 0 -> {
                applyHighAmountPattern(transaction);
                log.info("Applied fraud pattern: High Amount");
            }
            case 1 -> {
                applyOffHoursPattern(transaction);
                log.info("Applied fraud pattern: Off Hours");
            }
            case 2 -> {
                applySuspiciousRemittancePattern(transaction);
                log.info("Applied fraud pattern: Suspicious Remittance");
            }
            case 3 -> {
                applyCrossBorderHighRiskPattern(transaction);
                log.info("Applied fraud pattern: Cross-Border High Risk");
            }
        }
    }
    private void applyHighAmountPattern(Transaction transaction) {
        BankInfo fromBank = bankDataService.getRandomBank();
        BankInfo toBank = getDistinctToBank(fromBank);

        updateTransactionBanks(transaction, fromBank, toBank);

        BigDecimal amount = AmountGenerator.generateHigh();
        transaction.setAmount(amount);
        log.info("Applied HIGH_AMOUNT pattern with amount: {}", amount);
    }

    private void applyOffHoursPattern(Transaction transaction) {
        BankInfo fromBank = bankDataService.getRandomBank();
        BankInfo toBank = getDistinctToBank(fromBank);

        updateTransactionBanks(transaction, fromBank, toBank);

        LocalDateTime now = LocalDateTime.now();
        LocalTime offHoursTime = generateOffHoursTime();
        LocalDateTime offHoursDateTime = now.with(offHoursTime);

        transaction.setTimestamp(offHoursDateTime);
        transaction.setAmount(AmountGenerator.generateMedium());
        log.debug("Applied OFF_HOURS pattern at: {}", offHoursDateTime);
    }

    private void applySuspiciousRemittancePattern(Transaction transaction) {
        // Use BankDataService to get banks from CSV
        BankInfo fromBank = bankDataService.getRandomBank();
        BankInfo toBank = getDistinctToBank(fromBank);

        updateTransactionBanks(transaction, fromBank, toBank);

        transaction.setAmount(AmountGenerator.generateMedium());
        log.info("Applied SUSPICIOUS_REMITTANCE pattern");
    }

    private void applyCrossBorderHighRiskPattern(Transaction transaction) {
        String riskCountry = HIGH_RISK_COUNTRIES[random.nextInt(HIGH_RISK_COUNTRIES.length)];
        transaction.setToCountryCode(riskCountry);

        BigDecimal amount = random.nextBoolean() ?
                AmountGenerator.generateMedium() : AmountGenerator.generateHigh();
        transaction.setAmount(amount);
        log.info("Applied CROSS_BORDER_HIGH_RISK pattern to country: {} with amount: {}", riskCountry, amount);
    }

    @Override
    protected BigDecimal generateRandomAmount() {
        return AmountGenerator.generateMedium();
    }

    private LocalTime generateOffHoursTime() {
        int hour = random.nextBoolean() ?
                random.nextInt(5) + 23 :
                random.nextInt(6);

        if (hour >= 24) hour -= 24;

        int minute = random.nextInt(60);
        int second = random.nextInt(60);

        return LocalTime.of(hour, minute, second);
    }

    private void updateTransactionBanks(Transaction transaction, BankInfo fromBank, BankInfo toBank) {
        String fromAccount = generateAccountNumber();
        String toAccount = generateAccountNumber();
        String fromIBAN = generateIBANForBank(fromBank, fromAccount);
        String toIBAN = generateIBANForBank(toBank, toAccount);

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setFromBankSwift(fromBank.getSwiftCode());
        transaction.setToBankSwift(toBank.getSwiftCode());
        transaction.setFromBankName(fromBank.getBankName());
        transaction.setToBankName(toBank.getBankName());
        transaction.setFromIBAN(fromIBAN);
        transaction.setToIBAN(toIBAN);
        transaction.setFromCountryCode(fromBank.getCountryCode());
        transaction.setToCountryCode(toBank.getCountryCode());
        transaction.setCurrency(determineCurrency(fromBank, toBank));
    }
}