package com.toufik.trxgeneratorservice.mt103trx.service;

import java.math.BigDecimal;
import java.util.Random;

public enum AmountGenerationStrategy {

    /**
     * Normal distribution with realistic transaction patterns
     * 50% small amounts (10-1000), 30% medium (1000-10000), 20% large (10000-100000)
     */
    NORMAL {
        @Override
        public BigDecimal generateAmount(Random random) {
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
    },

    /**
     * Simple normal distribution for basic transaction generation
     * Range: 100-5100
     */
    SIMPLE_NORMAL {
        @Override
        public BigDecimal generateAmount(Random random) {
            double amount = 100.0 + (random.nextDouble() * 5000.0);
            return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
        }
    };

    /**
     * Generate amount based on the specific strategy
     */
    public abstract BigDecimal generateAmount(Random random);
}