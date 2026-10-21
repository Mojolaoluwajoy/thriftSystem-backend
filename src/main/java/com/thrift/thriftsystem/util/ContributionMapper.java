package com.thrift.thriftsystem.util;

import com.thrift.thriftsystem.dto.request.ContributionRequest;
import com.thrift.thriftsystem.dto.response.ContributionResponse;
import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.model.Contribution;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class ContributionMapper {

    public Contribution toModel(
            ContributionRequest contributionRequest,
            String userId,
            String membershipId,
            BigDecimal amount,
            LocalDateTime dueDate) {
        return Contribution.builder()
                .userId(userId)
                .groupId(contributionRequest.getGroupId())
                .membershipId(membershipId)
                .amount(amount)
                .currency(contributionRequest.getCurrency())
                .cycleNumber(contributionRequest.getCycleNumber())
                .dueDate(dueDate)
                .status(ContributionStatus.PENDING)
                .build();
    }

    public ContributionResponse toResponse(Contribution contribution) {
        return ContributionResponse.builder()
                .id(contribution.getId())
                .userId(contribution.getUserId())
                .groupId(contribution.getGroupId())
                .membershipId(contribution.getMembershipId())
                .amount(contribution.getAmount())
                .currency(contribution.getCurrency())
                .payStackReference(contribution.getPaystackReference())
                .status(contribution.getStatus())
                .cycleNumber(contribution.getCycleNumber())
                .dueDate(contribution.getDueDate())
                .paidAt(contribution.getPaidAt())
                .createdAt(contribution.getCreatedDate())
                .build();
    }
}
