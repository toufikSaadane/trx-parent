package com.toufik.trxalertservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toufik.trxalertservice.fraud.model.FraudAlert;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FraudAlertReport {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("reportGeneratedAt")
    private LocalDateTime reportGeneratedAt;

    @JsonProperty("totalAlerts")
    private int totalAlerts;

    @JsonProperty("alerts")
    private List<FraudAlert> alerts;
}