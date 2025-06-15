package com.toufik.trxgeneratorservice.mt103trx.model;

public enum FraudScenario {
    HIGH_AMOUNT_THRESHOLD("High Amount Transaction", 10),
    OFF_HOURS_TRANSACTION("Off Hours Transaction", 15),
    SUSPICIOUS_REMITTANCE("Suspicious Remittance Information", 20),
    ROUND_AMOUNT_PATTERN("Round Amount Pattern", 15),
    FREQUENT_SMALL_AMOUNTS("Frequent Small Amounts", 12),
    CROSS_BORDER_HIGH_RISK("Cross Border High Risk", 8),
    STRUCTURING_PATTERN("Structuring Pattern", 10),
    CRYPTOCURRENCY_KEYWORDS("Cryptocurrency Keywords", 10);

    private final String description;
    private final int weight;

    FraudScenario(String description, int weight) {
        this.description = description;
        this.weight = weight;
    }

    public String getDescription() {
        return description;
    }

    public int getWeight() {
        return weight;
    }
}