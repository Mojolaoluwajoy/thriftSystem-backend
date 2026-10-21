package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.integration.ExchangeRateClient;
import com.thrift.thriftsystem.model.Currency;
import com.thrift.thriftsystem.repository.CurrencyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock private CurrencyRepository currencyRepository;

    @Mock private ExchangeRateClient exchangeRateClient;

    @InjectMocks private CurrencyService currencyService;

    private Currency ngnCurrency;
    private Currency usdCurrency;

    @BeforeEach
    void setUp() {
        ngnCurrency =
                Currency.builder()
                        .id("ngn123")
                        .code("NGN")
                        .name("Nigerian Naira")
                        .symbol("₦")
                        .exchangeRate(BigDecimal.ONE)
                        .active(true)
                        .build();

        usdCurrency =
                Currency.builder()
                        .id("usd123")
                        .code("USD")
                        .name("US Dollar")
                        .symbol("$")
                        .exchangeRate(new BigDecimal("1500.00"))
                        .active(true)
                        .build();
    }

    @Test
    void convert_SameCurrency_ReturnsSameAmount() {
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal result = currencyService.convert(amount, "NGN", "NGN");
        assertEquals(amount, result);
    }

    @Test
    void convert_NGNToUSD_Success() {
        when(currencyRepository.findByCode("NGN")).thenReturn(Optional.of(ngnCurrency));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

        BigDecimal result = currencyService.convert(new BigDecimal("15000"), "NGN", "USD");

        assertNotNull(result);
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void convert_InvalidCurrency_ThrowsException() {
        when(currencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThrows(
                BadRequestException.class,
                () -> currencyService.convert(new BigDecimal("10000"), "XYZ", "NGN"));
    }

    @Test
    void updateExchangeRates_Success() {
        when(exchangeRateClient.getLatestRates(any()))
                .thenReturn(Map.of("conversion_rates", Map.of("USD", 0.00067, "GBP", 0.00053)));
        when(currencyRepository.findByActiveTrue()).thenReturn(List.of(ngnCurrency, usdCurrency));

        currencyService.updateExchangeRates();

        verify(currencyRepository, atLeastOnce()).save(any());
    }

    @Test
    void updateExchangeRates_ApiFails_NoException() {
        when(exchangeRateClient.getLatestRates(any()))
                .thenThrow(new RuntimeException("API unavailable"));

        assertDoesNotThrow(() -> currencyService.updateExchangeRates());
    }

    @Test
    void getActiveCurrencies_Success() {
        when(currencyRepository.findByActiveTrue()).thenReturn(List.of(ngnCurrency, usdCurrency));

        List<Currency> result = currencyService.getActiveCurrencies();

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
