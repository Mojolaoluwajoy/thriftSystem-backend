package com.thrift.thriftsystem.dto.response;

import com.thrift.thriftsystem.enums.ContributionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionResponse {

    private String id;
    private String userId;
    private String groupId;
    private String membershipId;
    private BigDecimal amount;
    private String currency;
    private String payStackReference;
    private ContributionStatus status;
    private Integer cycleNumber;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
