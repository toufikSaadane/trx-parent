package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.InvalidScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Handles selection of invalid scenarios with optional weighting
 */
@Component
@Slf4j
public class InvalidScenarioSelector {

    private final Random random = new Random();
    private final Map<InvalidScenario, Double> scenarioWeights;

    public InvalidScenarioSelector() {
        // Initialize default weights for different scenarios
        scenarioWeights = new HashMap<>();
        scenarioWeights.put(InvalidScenario.MISSING_MANDATORY_FIELDS, 0.20);
        scenarioWeights.put(InvalidScenario.INVALID_BIC_FORMAT, 0.15);
        scenarioWeights.put(InvalidScenario.INVALID_DATE_FORMAT, 0.15);
        scenarioWeights.put(InvalidScenario.INVALID_AMOUNT_FORMAT, 0.15);
        scenarioWeights.put(InvalidScenario.MISSING_HEADER_BLOCKS, 0.10);
        scenarioWeights.put(InvalidScenario.INVALID_FIELD_STRUCTURE, 0.10);
        scenarioWeights.put(InvalidScenario.TRUNCATED_MESSAGES, 0.10);
        scenarioWeights.put(InvalidScenario.INVALID_CHARACTERS, 0.05);
    }

    /**
     * Selects a random scenario uniformly
     */
    public InvalidScenario selectRandomScenario() {
        InvalidScenario[] scenarios = InvalidScenario.values();
        return scenarios[random.nextInt(scenarios.length)];
    }

    /**
     * Selects a scenario based on configured weights
     */
    public InvalidScenario selectWeightedScenario() {
        double totalWeight = scenarioWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;

        double cumulativeWeight = 0.0;
        for (Map.Entry<InvalidScenario, Double> entry : scenarioWeights.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue <= cumulativeWeight) {
                log.debug("Selected weighted scenario: {} (weight: {})", entry.getKey(), entry.getValue());
                return entry.getKey();
            }
        }

        // Fallback to last scenario if rounding errors occur
        InvalidScenario fallback = InvalidScenario.MISSING_MANDATORY_FIELDS;
        log.warn("Weighted selection fallback to: {}", fallback);
        return fallback;
    }

    /**
     * Updates the weight for a specific scenario
     */
    public void updateScenarioWeight(InvalidScenario scenario, double weight) {
        if (weight < 0 || weight > 1) {
            throw new IllegalArgumentException("Weight must be between 0 and 1");
        }
        scenarioWeights.put(scenario, weight);
        log.info("Updated weight for scenario {} to {}", scenario, weight);
    }

    /**
     * Gets the current weight for a scenario
     */
    public double getScenarioWeight(InvalidScenario scenario) {
        return scenarioWeights.getOrDefault(scenario, 0.0);
    }

    /**
     * Gets all scenario weights
     */
    public Map<InvalidScenario, Double> getAllWeights() {
        return new HashMap<>(scenarioWeights);
    }

    /**
     * Resets all weights to equal distribution
     */
    public void resetToEqualWeights() {
        double equalWeight = 1.0 / InvalidScenario.values().length;
        for (InvalidScenario scenario : InvalidScenario.values()) {
            scenarioWeights.put(scenario, equalWeight);
        }
        log.info("Reset all scenario weights to equal distribution: {}", equalWeight);
    }
}