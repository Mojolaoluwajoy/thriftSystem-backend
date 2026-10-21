package com.thrift.thriftsystem.dto.request;

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
public class PayoutRequest {

    private String userId;
    private String groupId;
    private String membershipId;
    private BigDecimal amount;
    private String currency;
    private Integer cycleNumber;
    private LocalDateTime scheduledDate;
}
