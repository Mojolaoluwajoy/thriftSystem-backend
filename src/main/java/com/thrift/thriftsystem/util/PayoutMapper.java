package com.thrift.thriftsystem.util;

import com.thrift.thriftsystem.dto.response.PayoutResponse;
import com.thrift.thriftsystem.enums.PayoutStatus;
import com.thrift.thriftsystem.model.Payout;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PayoutMapper {

    public Payout toModel(
            String userId,
            String groupId,
            String membershipId,
            BigDecimal amount,
            String currency,
            int cycleNumber,
            LocalDateTime payOutDate) {
        return Payout.builder()
                .userId(userId)
                .groupId(groupId)
                .membershipId(membershipId)
                .amount(amount)
                .currency(currency)
                .cycleNumber(cycleNumber)
                .scheduledDate(payOutDate)
                .status(PayoutStatus.PENDING)
                .build();
    }

    public PayoutResponse toResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .userId(payout.getUserId())
                .groupId(payout.getGroupId())
                .membershipId(payout.getMembershipId())
                .amount(payout.getAmount())
                .currency(payout.getCurrency())
                .payStackTransferCode(payout.getPayStackTransferCode())
                .payoutStatus(payout.getStatus())
                .failureReason(payout.getFailureReason())
                .cycleNumber(payout.getCycleNumber())
                .scheduledDate(payout.getScheduledDate())
                .processedAt(payout.getProcessedAt())
                .createdAt(payout.getCreatedDate())
                .build();
    }
}
