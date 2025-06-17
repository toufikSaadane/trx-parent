package com.toufik.trxalertservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class FraudConfigurationService {

    @Value("${fraud.detection.threshold:70}")
    private int fraudThreshold;

    @Value("${fraud.detection.high-amount-threshold:15000.00}")
    private BigDecimal highAmountThreshold;

    @Value("${fraud.detection.detectors.high-amount.enabled:true}")
    private boolean highAmountDetectorEnabled;

    @Value("${fraud.detection.detectors.off-hours.enabled:true}")
    private boolean offHoursDetectorEnabled;

    @Value("${fraud.detection.detectors.suspicious-remittance.enabled:true}")
    private boolean suspiciousRemittanceDetectorEnabled;

    @Value("${fraud.detection.detectors.round-amount.enabled:true}")
    private boolean roundAmountDetectorEnabled;

    @Value("${fraud.detection.detectors.cross-border.enabled:true}")
    private boolean crossBorderDetectorEnabled;

    @Value("${fraud.detection.detectors.structuring.enabled:true}")
    private boolean structuringDetectorEnabled;

    @Value("${fraud.detection.detectors.cryptocurrency.enabled:true}")
    private boolean cryptoDetectorEnabled;

    public int getFraudThreshold() {
        return fraudThreshold;
    }

    public Optional<BigDecimal> getHighAmountThreshold() {
        return Optional.ofNullable(highAmountThreshold);
    }

    public boolean isDetectorEnabled(String detectorName) {
        return switch (detectorName) {
            case "HIGH_AMOUNT_THRESHOLD" -> highAmountDetectorEnabled;
            case "OFF_HOURS_TRANSACTION" -> offHoursDetectorEnabled;
            case "SUSPICIOUS_REMITTANCE" -> suspiciousRemittanceDetectorEnabled;
            case "ROUND_AMOUNT_PATTERN" -> roundAmountDetectorEnabled;
            case "CROSS_BORDER_HIGH_RISK" -> crossBorderDetectorEnabled;
            case "STRUCTURING_PATTERN" -> structuringDetectorEnabled;
            case "CRYPTOCURRENCY_KEYWORDS" -> cryptoDetectorEnabled;
            default -> true;
        };
    }
}
