package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.FraudScenario;
import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class FraudTransactionFactory extends BaseTransactionFactory {

    private static final String[] HIGH_RISK_COUNTRIES = {
            "AF", "IR", "KP", "MM", "SY", "YE"};

    private final Random random = new Random();

    /**
     * Creates a fraud transaction based on the specified scenario
     */
    public Transaction createFraudTransaction(FraudScenario scenario) {
        Transaction transaction = createBaseTransaction();

        // Apply fraud scenario-specific modifications
        applyFraudScenario(transaction, scenario);

        log.debug("Created fraud transaction for scenario: {} with amount: {}",
                scenario.name(), transaction.getAmount());

        return transaction;
    }

    /**
     * Applies fraud scenario characteristics to the transaction
     */
    private void applyFraudScenario(Transaction transaction, FraudScenario scenario) {
        switch (scenario) {
            case HIGH_AMOUNT_THRESHOLD -> applyHighAmountScenario(transaction);
            case OFF_HOURS_TRANSACTION -> applyOffHoursScenario(transaction);
            case SUSPICIOUS_REMITTANCE -> applySuspiciousRemittanceScenario(transaction);
            case CROSS_BORDER_HIGH_RISK -> applyCrossBorderHighRiskScenario(transaction);
        }
    }

    private void applyHighAmountScenario(Transaction transaction) {
        BigDecimal amount = AmountGenerator.generateHigh();
        transaction.setAmount(amount);
    }

    private void applyOffHoursScenario(Transaction transaction) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime offHoursTime = generateOffHoursTime();
        LocalDateTime offHoursDateTime = now.with(offHoursTime);

        transaction.setTimestamp(offHoursDateTime);
        transaction.setAmount(AmountGenerator.generateMedium());
    }

    private void applySuspiciousRemittanceScenario(Transaction transaction) {
        transaction.setAmount(AmountGenerator.generateMedium());
    }

    private void applyCrossBorderHighRiskScenario(Transaction transaction) {
        // Set destination to high-risk country
        String riskCountry = HIGH_RISK_COUNTRIES[random.nextInt(HIGH_RISK_COUNTRIES.length)];
        transaction.setToCountryCode(riskCountry);

        // Generate medium to high amounts for cross-border
        BigDecimal amount = random.nextBoolean() ?
                AmountGenerator.generateMedium() : AmountGenerator.generateHigh();
        transaction.setAmount(amount);
    }

    @Override
    protected BigDecimal generateRandomAmount() {
        return AmountGenerator.generateMedium();
    }

    /**
     * Generates off-hours time (between 11 PM and 5 AM)
     */
    private LocalTime generateOffHoursTime() {
        int hour = random.nextBoolean() ?
                random.nextInt(5) + 23 :
                random.nextInt(6);

        if (hour >= 24) hour -= 24; // Handle wrap-around

        int minute = random.nextInt(60);
        int second = random.nextInt(60);

        return LocalTime.of(hour, minute, second);
    }
}