package com.toufik.trxalertservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fraud.email")
@Data
public class EmailConfig {
    private String recipientEmail = "toufik.saadane@gmail.com";
    private String senderEmail = "fraud-alert@company.com";
    private String senderName = "Transaction Fraud Alert System";
    private String subject = "🚨 FRAUD ALERT - Suspicious Transaction Detected";
}