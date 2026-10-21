package com.thrift.thriftsystem.model;

import com.thrift.thriftsystem.enums.PayoutStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payouts")
public class Payout {

    @Id private String id;

    private String userId;
    private String groupId;
    private String membershipId;

    private BigDecimal amount;
    private String currency;

    private String payStackTransferCode;
    private String payStackTransferType;
    private String payStackRecipientCode;
    private String transferReference;
    private String failureReason;

    @Builder.Default private PayoutStatus status = PayoutStatus.PENDING;

    private Integer cycleNumber;
    private LocalDateTime scheduledDate;
    private LocalDateTime processedAt;

    @CreatedDate private LocalDateTime createdDate;

    @CreatedDate private LocalDateTime updatedDate;
}
