package com.thrift.thriftsystem.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thrift.thriftsystem.service.PaymentService;
import com.thrift.thriftsystem.service.PayoutService;
import com.thrift.thriftsystem.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock private PaymentService paymentService;

    @Mock private PayoutService payoutService;

    @Mock private WebhookService webhookService;

    private WebhookController webhookController;

    @BeforeEach
    void setUp() {
        webhookController =
                new WebhookController(
                        paymentService, payoutService, webhookService, new ObjectMapper());
    }

    @Test
    void invalidSignature_Returns401AndDoesNothing() {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"THRIFT-123\"}}";
        when(webhookService.isValidSignature(payload, "bad")).thenReturn(false);

        ResponseEntity<String> response = webhookController.handlePaystackWebhook("bad", payload);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(paymentService, payoutService);
    }

    @Test
    void chargeSuccess_RecordsContributionPayment() {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"THRIFT-123\"}}";
        when(webhookService.isValidSignature(payload, "sig")).thenReturn(true);

        ResponseEntity<String> response = webhookController.handlePaystackWebhook("sig", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService).handlePaymentSuccess("THRIFT-123");
        verifyNoInteractions(payoutService);
    }

    @Test
    void transferSuccess_UpdatesPayout() {
        String payload = "{\"event\":\"transfer.success\",\"data\":{\"reference\":\"payout-abc\"}}";
        when(webhookService.isValidSignature(payload, "sig")).thenReturn(true);

        ResponseEntity<String> response = webhookController.handlePaystackWebhook("sig", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(payoutService).handleTransferEvent("transfer.success", "payout-abc", null);
        verifyNoInteractions(paymentService);
    }

    @Test
    void transferFailed_PassesReasonToPayout() {
        String payload =
                "{\"event\":\"transfer.failed\",\"data\":{\"reference\":\"payout-abc\","
                        + "\"reason\":\"Account closed\"}}";
        when(webhookService.isValidSignature(payload, "sig")).thenReturn(true);

        webhookController.handlePaystackWebhook("sig", payload);

        verify(payoutService)
                .handleTransferEvent("transfer.failed", "payout-abc", "Account closed");
    }

    @Test
    void unrelatedEvent_IsAcknowledgedAndIgnored() {
        String payload = "{\"event\":\"subscription.create\",\"data\":{}}";
        when(webhookService.isValidSignature(payload, "sig")).thenReturn(true);

        ResponseEntity<String> response = webhookController.handlePaystackWebhook("sig", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(paymentService, payoutService);
    }

    @Test
    void processingError_Returns500SoPaystackRetries() {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"THRIFT-123\"}}";
        when(webhookService.isValidSignature(payload, "sig")).thenReturn(true);
        doThrow(new RuntimeException("db down")).when(paymentService).handlePaymentSuccess(any());

        ResponseEntity<String> response = webhookController.handlePaystackWebhook("sig", payload);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
