package com.thrift.thriftsystem.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thrift.thriftsystem.service.PaymentService;
import com.thrift.thriftsystem.service.PayoutService;
import com.thrift.thriftsystem.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;
    private final PayoutService payoutService;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;


    @PostMapping("/paystack")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestHeader("x-paystack-signature") String signature,
            @RequestBody String rawPayload) {
        try {
            // Paystack signs the raw request body, so validate before parsing
            if (!webhookService.isValidSignature(rawPayload, signature)) {
                log.error("Invalid webhook signature");
                return new ResponseEntity<>("Invalid signature",
                        HttpStatus.UNAUTHORIZED);
            }

            Map<String, Object> payload = objectMapper.readValue(
                    rawPayload, new TypeReference<Map<String, Object>>() {});

            String event = (String) payload.get("event");
            Map<String, Object> data =
                    (Map<String, Object>) payload.get("data");

            if ("charge.success".equals(event)) {
                String reference = (String) data.get("reference");
                paymentService.handlePaymentSuccess(reference);
                log.info("Webhook processed for reference: {}", reference);
            } else if (event != null && event.startsWith("transfer.")) {
                String reference = (String) data.get("reference");
                payoutService.handleTransferEvent(event, reference,
                        (String) data.get("reason"));
                log.info("Transfer webhook processed for reference: {}", reference);
            }

            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            return new ResponseEntity<>("Error",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}