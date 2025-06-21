package com.toufik.trxalertservice.service;

import com.toufik.trxalertservice.fraud.model.FraudAlert;
import com.toufik.trxalertservice.fraud.FraudAlertNotificationService;
import com.toufik.trxalertservice.fraud.service.FraudDetectionEngine;
import com.toufik.trxalertservice.model.TransactionWithMT103Event;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionAlertConsumerService {

    private final FraudDetectionEngine fraudDetectionEngine;
    private final FraudAlertNotificationService fraudAlertNotificationService;

    @KafkaListener(
            topics = "transaction_alert",
            groupId = "transaction-alert-group",
            properties = {
                    "auto.offset.reset=latest"
            }
    )
    public void consumeTransactionAlert(@Payload TransactionWithMT103Event transactionWithMT103Event) throws MessagingException, UnsupportedEncodingException {
        log.info("======================= ALERT SERVICE RECEIVED TRANSACTION =============================");


        List<FraudAlert> fraudAlerts = fraudDetectionEngine.detectFraud(transactionWithMT103Event);
        fraudAlertNotificationService.sendFraudAlerts(fraudAlerts);
    }
}