package com.toufik.trxalertservice.fraud;

import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.fraud.service.FraudAlertEmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudAlertNotificationService {

    private final FraudAlertEmailService emailService;

    public void sendFraudAlerts(List<FraudAlert> alerts) throws MessagingException, UnsupportedEncodingException {
            emailService.sendFraudAlertEmail(alerts);
    }
}