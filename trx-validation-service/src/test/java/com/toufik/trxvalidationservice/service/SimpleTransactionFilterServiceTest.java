package com.toufik.trxvalidationservice.service;

import com.toufik.trxvalidationservice.model.Transaction;
import com.toufik.trxvalidationservice.model.TransactionWithMT103Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleTransactionFilterServiceTest {

    @Mock
    private TransactionProducerService producerService;

    @InjectMocks
    private SimpleTransactionFilterService filterService;

    @Test
    void shouldNotSendAlert_WhenContentIsNull() {

        TransactionWithMT103Event event = createEvent(null);
        filterService.process(event);
        verify(producerService, never()).sendTransactionAlert(any());
    }

    @Test
    void shouldNotSendAlert_WhenContentIsEmpty() {

        TransactionWithMT103Event event = createEvent("");
        filterService.process(event);
        verify(producerService, never()).sendTransactionAlert(any());
    }

    @Test
    void shouldNotSendAlert_WhenContentIsInvalid() {

        TransactionWithMT103Event event = createEvent("invalid content");
        filterService.process(event);
        verify(producerService, never()).sendTransactionAlert(any());
    }

    @Test
    void shouldSendAlert_WhenContentIsValid() {

        String validContent = createMinimalValidMT103();
        TransactionWithMT103Event event = createEvent(validContent);
        filterService.process(event);

        verify(producerService, times(1)).sendTransactionAlert(event);
    }

    private String createMinimalValidMT103() {
        // Create the simplest possible valid MT103 that passes all checks
        return "{1:F01COBADEFFXXX0}{2:I103UNCRITMMXXX0N}{3:{108:cd6d508c-5049-4a}}\n" +
                "{4:\n" +
                ":20:cd6d508c-5049-4a\n" +
                ":23B:CRED\n" +
                ":32A:250622EUR38329,19\n" +
                ":33B:EUR38329,19\n" +
                ":71A:SHA\n" +
                ":50K:/220576400523\n" +
                "10040000\n" +
                "456 Business Ave\n" +
                "Frankfurt, Germany\n" +
                ":52A:COBADEFF\n" +
                ":53B:/COBADEFF\n" +
                ":56A:UNCRITMM XXX\n" +
                ":57A:UNCRITMM\n" +
                ":59:/201093193710\n" +
                "02008\n" +
                "456 Business Ave\n" +
                "Milan, Italy\n" +
                ":70:Payment for services - TXN ID: cd6d508c - Cross-border transfer\n" +
                ":72:/INS/COBADEFF\n" +
                "}\n" +
                "{5:{MAC:9A90B885}{CHK:E065669BF6C5}}";
    }

    private TransactionWithMT103Event createEvent(String mt103Content) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-123");

        TransactionWithMT103Event event = new TransactionWithMT103Event();
        event.setTransaction(transaction);
        event.setMt103Content(mt103Content);

        return event;
    }
}