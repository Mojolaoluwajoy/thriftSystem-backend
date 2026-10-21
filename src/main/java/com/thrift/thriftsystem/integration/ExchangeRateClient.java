package com.thrift.thriftsystem.integration;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "exchangeRateClient", url = "${EXCHANGE_RATE_BASE_URL}")
public interface ExchangeRateClient {

    @GetMapping("/{apiKey}/latest/NGN")
    Map<String, Object> getLatestRates(@PathVariable String apiKey);
}
