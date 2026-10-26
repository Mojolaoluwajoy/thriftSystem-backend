package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.integration.PaystackClient;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackService {

    private final PaystackClient paystackClient;

    // Paystack sends the member back here after checkout
    @Value("${app.frontend-url:}")
    private String frontendUrl;

    public Map<String, Object> initializePayment(
            String email, BigDecimal amount, String reference) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("email", email);
            // Paystack expects amount in kobo (multiply by 100)
            request.put("amount", toKobo(amount));
            request.put("reference", reference);
            request.put("currency", "NGN");
            if (frontendUrl != null && !frontendUrl.isBlank()) {
                request.put("callback_url", frontendUrl + "/?payment=complete");
            }

            Map<String, Object> response = paystackClient.initializeTransaction(request);

            log.info("Payment initialized for: {}", email);
            return response;
        } catch (Exception e) {
            log.error("Payment initialization failed: {}", e.getMessage());
            throw new BadRequestException("Payment initialization failed");
        }
    }

    public boolean verifyPayment(String reference) {
        try {
            Map<String, Object> response = paystackClient.verifyTransaction(reference);

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            if (data != null) {
                String status = (String) data.get("status");
                return "success".equals(status);
            }
            return false;
        } catch (Exception e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> listBanks() {
        try {
            Map<String, Object> response = paystackClient.listBanks("nigeria", "NGN");
            List<Map<String, Object>> banks = (List<Map<String, Object>>) response.get("data");
            if (banks == null) {
                return List.of();
            }
            // Only what the bank picker needs
            return banks.stream()
                    .map(b -> Map.<String, Object>of("name", b.get("name"), "code", b.get("code")))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list banks: {}", e.getMessage());
            throw new BadRequestException("Could not load banks");
        }
    }

    public Map<String, Object> createTransferRecipient(
            String name, String accountNumber, String bankCode) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("type", "nuban");
            request.put("name", name);
            request.put("account_number", accountNumber);
            request.put("bank_code", bankCode);
            request.put("currency", "NGN");

            Map<String, Object> response = paystackClient.createTransferRecipient(request);

            log.info("Transfer recipient created for: {}", name);
            return response;
        } catch (Exception e) {
            log.error("Failed to create transfer recipient: {}", e.getMessage());
            throw new BadRequestException("Failed to create transfer recipient");
        }
    }

    public Map<String, Object> initiateTransfer(
            String recipientCode, BigDecimal amount, String reason, String reference) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("source", "balance");
            request.put("amount", toKobo(amount));
            request.put("recipient", recipientCode);
            request.put("reason", reason);
            // Paystack rejects a reused reference, so retries can't pay twice
            request.put("reference", reference);

            Map<String, Object> response = paystackClient.initiateTransfer(request);

            log.info("Transfer initiated: {}", reason);
            return response;
        } catch (Exception e) {
            log.error("Transfer initiation failed: {}", e.getMessage());
            throw new BadRequestException("Transfer initiation failed");
        }
    }

    public boolean refundPayment(String reference) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("transaction", reference);
            Map<String, Object> response = paystackClient.createRefund(request);
            log.info("Refund requested for: {}", reference);
            return Boolean.TRUE.equals(response.get("status"));
        } catch (Exception e) {
            log.error("Refund failed for {}: {}", reference, e.getMessage());
            return false;
        }
    }

    // Paystack expects amounts in kobo; long avoids overflow on large pots
    private long toKobo(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    public String generateReference() {
        // A timestamp alone can collide when two members pay in the same millisecond
        return "THRIFT-" + java.util.UUID.randomUUID();
    }
}
