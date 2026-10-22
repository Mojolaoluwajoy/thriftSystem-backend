package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WebhookServiceTest {

    private static final String SECRET = "sk_test_webhook_secret";
    private static final String PAYLOAD =
            "{\"event\":\"charge.success\",\"data\":{\"reference\":\"THRIFT-123\"}}";

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService();
        ReflectionTestUtils.setField(webhookService, "paystackSecretKey", SECRET);
    }

    static String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void isValidSignature_CorrectSignature_ReturnsTrue() throws Exception {
        assertTrue(webhookService.isValidSignature(PAYLOAD, sign(PAYLOAD, SECRET)));
    }

    @Test
    void isValidSignature_WrongSecret_ReturnsFalse() throws Exception {
        assertFalse(webhookService.isValidSignature(PAYLOAD, sign(PAYLOAD, "sk_test_other")));
    }

    @Test
    void isValidSignature_TamperedPayload_ReturnsFalse() throws Exception {
        String signature = sign(PAYLOAD, SECRET);
        String tampered = PAYLOAD.replace("THRIFT-123", "THRIFT-999");

        assertFalse(webhookService.isValidSignature(tampered, signature));
    }

    @Test
    void isValidSignature_ReformattedJson_ReturnsFalse() throws Exception {
        // The signature covers the exact bytes Paystack sent, so re-serialised JSON must not pass
        String signature = sign(PAYLOAD, SECRET);
        String reformatted = PAYLOAD.replace(",", ", ");

        assertFalse(webhookService.isValidSignature(reformatted, signature));
    }

    @Test
    void isValidSignature_MissingSignature_ReturnsFalse() {
        assertFalse(webhookService.isValidSignature(PAYLOAD, null));
    }
}
