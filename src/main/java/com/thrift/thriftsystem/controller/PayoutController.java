package com.thrift.thriftsystem.controller;

import com.thrift.thriftsystem.dto.response.ApiResponse;
import com.thrift.thriftsystem.dto.response.PayoutResponse;
import com.thrift.thriftsystem.service.PayoutService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping("/schedule")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> schedulePayout(
            @RequestParam String groupId,
            @RequestParam String userId,
            @RequestParam Integer cycleNumber) {
        PayoutResponse response = payoutService.schedulePayout(groupId, userId, cycleNumber);
        return new ResponseEntity<>(
                ApiResponse.success("Payout scheduled", response), HttpStatus.CREATED);
    }

    @PostMapping("/{payoutId}/process")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> processPayout(@PathVariable String payoutId) {
        PayoutResponse response = payoutService.processPayout(payoutId);
        return new ResponseEntity<>(
                ApiResponse.success("Payout processing", response), HttpStatus.OK);
    }

    @GetMapping("/my-payouts")
    public ResponseEntity<ApiResponse> getMyPayouts() {
        List<PayoutResponse> response = payoutService.getMyPayouts();
        return new ResponseEntity<>(
                ApiResponse.success("Payouts retrieved", response), HttpStatus.OK);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse> getGroupPayouts(@PathVariable String groupId) {
        List<PayoutResponse> response = payoutService.getGroupPayouts(groupId);
        return new ResponseEntity<>(
                ApiResponse.success("Payouts retrieved", response), HttpStatus.OK);
    }
}
