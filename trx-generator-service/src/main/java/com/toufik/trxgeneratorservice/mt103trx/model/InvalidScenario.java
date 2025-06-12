package com.toufik.trxgeneratorservice.mt103trx.model;

/**
 * Enum representing different types of invalid MT103 scenarios
 */
public enum InvalidScenario {
    MISSING_MANDATORY_FIELDS("Missing mandatory fields like :20:, :23B:, or :32A:"),
    INVALID_BIC_FORMAT("Invalid BIC format or structure"),
    INVALID_DATE_FORMAT("Invalid date format in date fields"),
    INVALID_AMOUNT_FORMAT("Invalid amount format or structure"),
    MISSING_HEADER_BLOCKS("Missing required header blocks"),
    INVALID_FIELD_STRUCTURE("Invalid field separators or structure"),
    TRUNCATED_MESSAGES("Truncated or incomplete messages"),
    INVALID_CHARACTERS("Invalid control characters in message");

    private final String description;

    InvalidScenario(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}