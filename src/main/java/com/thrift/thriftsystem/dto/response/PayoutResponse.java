package com.thrift.thriftsystem.dto.response;

import com.thrift.thriftsystem.enums.PayoutStatus;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PayoutResponse {

    private String id;
    private String userId;
    private String  groupId;
    private String membershipId;
    private BigDecimal amount;
    private String currency;
    private String payStackTransferCode;
    private PayoutStatus payoutStatus;
    private String failureReason;
    private Integer cycleNumber;
    private LocalDateTime scheduledDate;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
