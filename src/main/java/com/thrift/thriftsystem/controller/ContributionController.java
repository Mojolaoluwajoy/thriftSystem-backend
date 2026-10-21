package com.thrift.thriftsystem.controller;

import com.thrift.thriftsystem.dto.request.ContributionRequest;
import com.thrift.thriftsystem.dto.response.ApiResponse;
import com.thrift.thriftsystem.dto.response.ContributionResponse;
import com.thrift.thriftsystem.service.ContributionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
public class ContributionController {
    private final ContributionService contributionService;

    @PostMapping
    public ResponseEntity<ApiResponse> initiateContribution(
            @RequestBody ContributionRequest contributionRequest) {
        ContributionResponse response =
                contributionService.initiateContribution(contributionRequest);
        return new ResponseEntity<>(
                ApiResponse.success("Contribution initiated", response), HttpStatus.CREATED);
    }

    @GetMapping("/my-contributions")
    public ResponseEntity<ApiResponse> getContribution() {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Your Contributions loaded", contributionService.getMyContributions()),
                HttpStatus.OK);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse> getGroupContribution(@PathVariable String groupId) {
        List<ContributionResponse> response = contributionService.getGroupContributions(groupId);
        return new ResponseEntity<>(
                ApiResponse.success("Group Contributions loaded", response), HttpStatus.OK);
    }
}
