package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.integration.ExchangeRateClient;
import com.thrift.thriftsystem.model.Currency;
import com.thrift.thriftsystem.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateClient exchangeRateClient;

    @Value("${EXCHANGE_RATE_API_KEY}")
    private String apiKey;

    public BigDecimal convert(BigDecimal amount, String fromCurrency,
                              String toCurrency) {
        if (fromCurrency.equals(toCurrency)) return amount;

        Currency from = currencyRepository.findByCode(fromCurrency)
                .orElseThrow(() -> new BadRequestException(
                        "Currency not found: " + fromCurrency));

        Currency to = currencyRepository.findByCode(toCurrency)
                .orElseThrow(() -> new BadRequestException(
                        "Currency not found: " + toCurrency));

        BigDecimal inNgn = amount.multiply(from.getExchangeRate());
        return inNgn.divide(to.getExchangeRate(), 2, RoundingMode.HALF_UP);
    }

    public void updateExchangeRates() {
        try {
            Map<String, Object> response =
                    exchangeRateClient.getLatestRates(apiKey);

            if (response != null && response.get("conversion_rates") != null) {
                Map<String, Object> rates =
                        (Map<String, Object>) response.get("conversion_rates");

                List<Currency> currencies = currencyRepository.findByActiveTrue();
                currencies.forEach(currency -> {
                    if (rates.containsKey(currency.getCode())) {
                        Object rateObj = rates.get(currency.getCode());
                        BigDecimal rate = new BigDecimal(rateObj.toString());
                        currency.setExchangeRate(rate);
                        currencyRepository.save(currency);
                        log.info("Rate updated for: {}", currency.getCode());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to update exchange rates: {}", e.getMessage());
        }
    }

    public List<Currency> getActiveCurrencies() {
        return currencyRepository.findByActiveTrue();
    }
}