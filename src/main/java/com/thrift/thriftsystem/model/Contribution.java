package com.thrift.thriftsystem.model;

import com.thrift.thriftsystem.enums.ContributionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contributions")
public class Contribution {

    @Id private String id;

    private String userId;
    private String groupId;
    private String membershipId;

    private BigDecimal amount;
    private String currency;
    private Integer cycleNumber;
    // @CreatedDate
    // private LocalDateTime createdDate;
    @LastModifiedDate private String paystackReference;
    private String paystackTransactionId;

    @Builder.Default private ContributionStatus status = ContributionStatus.PENDING;

    private LocalDateTime dueDate;
    private LocalDateTime paidAt;

    @CreatedDate private LocalDateTime createdDate;
    @CreatedDate private LocalDateTime updatedDate;
}
