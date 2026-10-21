package com.thrift.thriftsystem.controller;

import com.thrift.thriftsystem.dto.response.ApiResponse;
import com.thrift.thriftsystem.service.PaymentService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize/{contributionId}")
    public ResponseEntity<ApiResponse> initializePayment(@PathVariable String contributionId) {
        Map<String, Object> response = paymentService.initializeContributionPayment(contributionId);
        return new ResponseEntity<>(
                ApiResponse.success("Payment initialized", response), HttpStatus.OK);
    }
}
