package com.thrift.thriftsystem.controller;

import com.thrift.thriftsystem.dto.response.ApiResponse;
import com.thrift.thriftsystem.model.Currency;
import com.thrift.thriftsystem.service.CurrencyService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public ResponseEntity<ApiResponse> getActiveCurrencies() {
        List<Currency> response = currencyService.getActiveCurrencies();
        return new ResponseEntity<>(
                ApiResponse.success("Currencies retrieved", response), HttpStatus.OK);
    }

    @GetMapping("/convert")
    public ResponseEntity<ApiResponse> convertCurrency(
            @RequestParam BigDecimal amount, @RequestParam String from, @RequestParam String to) {
        BigDecimal result = currencyService.convert(amount, from, to);
        return new ResponseEntity<>(
                ApiResponse.success("Conversion successful", result), HttpStatus.OK);
    }
}
