package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.config.FraudConfigurationService;
import com.toufik.trxalertservice.model.FraudAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
public abstract class AbstractFraudDetector implements FraudDetector {

    @Autowired
    protected FraudConfigurationService configurationService;

    @Override
    public boolean isEnabled() {
        return configurationService.isDetectorEnabled(getDetectorName());
    }

    protected FraudAlert createAlert(FraudAlert.FraudType fraudType, String details, int severity) {
        return FraudAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .fraudType(fraudType)
                .description(fraudType.getDescription())
                .details(details)
                .severity(severity)
                .timestamp(LocalDateTime.now())
                .status(FraudAlert.AlertStatus.ACTIVE)
                .build();
    }

    @Override
    public int getPriority() {
        return 1; // Default priority
    }
}
