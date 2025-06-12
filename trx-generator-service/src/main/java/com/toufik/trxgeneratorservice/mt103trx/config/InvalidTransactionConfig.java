package com.toufik.trxgeneratorservice.mt103trx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for invalid transaction generation
 */
@Configuration
@ConfigurationProperties(prefix = "invalid-transaction")
@Data
public class InvalidTransactionConfig {

    /**
     * Generation interval in milliseconds (default: 15 seconds)
     */
    private long generationInterval = 15000;

    /**
     * Whether to use weighted scenario selection
     */
    private boolean useWeightedSelection = true;

    /**
     * Maximum number of retry attempts for generation
     */
    private int maxRetryAttempts = 3;

    /**
     * Whether to log detailed MT103 content
     */
    private boolean logDetailedContent = false;

    /**
     * Maximum length of MT103 content to log
     */
    private int maxLogContentLength = 200;

    /**
     * Custom weights for different invalid scenarios
     * If not specified, default weights will be used
     */
    private Map<String, Double> scenarioWeights;

    /**
     * Whether the scheduled generation is enabled
     */
    private boolean scheduledGenerationEnabled = true;

    /**
     * Batch size for bulk generation operations
     */
    private int batchSize = 1;

    /**
     * Timeout for transaction sending operations in milliseconds
     */
    private long sendTimeoutMs = 5000;
}