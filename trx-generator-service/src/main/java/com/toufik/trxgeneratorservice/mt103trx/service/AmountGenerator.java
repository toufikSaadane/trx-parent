package com.toufik.trxgeneratorservice.mt103trx.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountGenerator {

    public static BigDecimal generateMedium() {
        return randomBetween(10_000, 100_000);
    }

    public static BigDecimal generateHigh() {
        return randomBetween(1_000_000, 10_000_000);
    }

    private static BigDecimal randomBetween(int min, int max) {
        double value = min + Math.random() * (max - min);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
