package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.integration.PaystackClient;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaystackServiceTest {

    @Mock private PaystackClient paystackClient;

    @InjectMocks private PaystackService paystackService;

    @BeforeEach
    void setUp() {}

    @Test
    void initializePayment_Success() {
        when(paystackClient.initializeTransaction(any()))
                .thenReturn(
                        Map.of(
                                "status",
                                true,
                                "data",
                                Map.of(
                                        "authorization_url",
                                        "https://paystack.com/pay/test",
                                        "reference",
                                        "THRIFT-123")));

        Map<String, Object> result =
                paystackService.initializePayment(
                        "john@example.com", new BigDecimal("10000"), "THRIFT-123");

        assertNotNull(result);
        verify(paystackClient, times(1)).initializeTransaction(any());
    }

    @Test
    void initializePayment_ApiFails_ThrowsException() {
        when(paystackClient.initializeTransaction(any()))
                .thenThrow(new RuntimeException("API error"));

        assertThrows(
                BadRequestException.class,
                () ->
                        paystackService.initializePayment(
                                "john@example.com", new BigDecimal("10000"), "THRIFT-123"));
    }

    @Test
    void verifyPayment_Success() {
        when(paystackClient.verifyTransaction(any()))
                .thenReturn(Map.of("status", true, "data", Map.of("status", "success")));

        boolean result = paystackService.verifyPayment("THRIFT-123");

        assertTrue(result);
        verify(paystackClient, times(1)).verifyTransaction(any());
    }

    @Test
    void verifyPayment_Failed() {
        when(paystackClient.verifyTransaction(any()))
                .thenReturn(Map.of("status", true, "data", Map.of("status", "failed")));

        boolean result = paystackService.verifyPayment("THRIFT-123");

        assertFalse(result);
    }

    @Test
    void verifyPayment_ApiFails_ReturnsFalse() {
        when(paystackClient.verifyTransaction(any())).thenThrow(new RuntimeException("API error"));

        boolean result = paystackService.verifyPayment("THRIFT-123");

        assertFalse(result);
    }

    @Test
    void createTransferRecipient_Success() {
        when(paystackClient.createTransferRecipient(any()))
                .thenReturn(Map.of("status", true, "data", Map.of("recipient_code", "RCP_123")));

        Map<String, Object> result =
                paystackService.createTransferRecipient("John Doe", "0123456789", "058");

        assertNotNull(result);
        verify(paystackClient, times(1)).createTransferRecipient(any());
    }

    @Test
    void initiateTransfer_Success() {
        when(paystackClient.initiateTransfer(any()))
                .thenReturn(Map.of("status", true, "data", Map.of("transfer_code", "TRF_123")));

        Map<String, Object> result =
                paystackService.initiateTransfer(
                        "RCP_123", new BigDecimal("10000"), "Thrift payout", "payout-123");

        assertNotNull(result);
        verify(paystackClient, times(1)).initiateTransfer(any());
    }

    @Test
    void generateReference_NotNull() {
        String reference = paystackService.generateReference();
        assertNotNull(reference);
        assertTrue(reference.startsWith("THRIFT-"));
    }
}
