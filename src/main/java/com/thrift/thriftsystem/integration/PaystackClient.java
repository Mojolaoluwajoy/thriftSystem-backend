package com.thrift.thriftsystem.integration;

import com.thrift.thriftsystem.config.PaystackFeignConfig;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "paystackClient",
        url = "${PAYSTACK_BASE_URL}",
        configuration = PaystackFeignConfig.class)
public interface PaystackClient {

    @PostMapping("/transaction/initialize")
    Map<String, Object> initializeTransaction(@RequestBody Map<String, Object> request);

    @GetMapping("/bank")
    Map<String, Object> listBanks(@RequestParam String country, @RequestParam String currency);

    @GetMapping("/transaction/verify/{reference}")
    Map<String, Object> verifyTransaction(@PathVariable String reference);

    @PostMapping("/transferrecipient")
    Map<String, Object> createTransferRecipient(@RequestBody Map<String, Object> request);

    @PostMapping("/refund")
    Map<String, Object> createRefund(@RequestBody Map<String, Object> request);

    @PostMapping("/transfer")
    Map<String, Object> initiateTransfer(@RequestBody Map<String, Object> request);
}
