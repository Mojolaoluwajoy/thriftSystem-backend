package com.thrift.thriftsystem.model;

import com.thrift.thriftsystem.enums.ContributionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private String paystackReference;

    // References of earlier checkouts for this contribution; their links stay payable, so
    // webhooks for them must still find this contribution
    @Builder.Default private List<String> previousPaystackReferences = new ArrayList<>();

    // Extra payments for an already-paid contribution, refunded through Paystack
    @Builder.Default private List<String> refundedPaystackReferences = new ArrayList<>();
    private String paystackTransactionId;

    @Builder.Default private ContributionStatus status = ContributionStatus.PENDING;

    private LocalDateTime dueDate;
    private LocalDateTime paidAt;

    @CreatedDate private LocalDateTime createdDate;
    @LastModifiedDate private LocalDateTime updatedDate;

    // Documents saved before these fields existed load them as null
    public List<String> getPreviousPaystackReferences() {
        if (previousPaystackReferences == null) {
            previousPaystackReferences = new ArrayList<>();
        }
        return previousPaystackReferences;
    }

    public List<String> getRefundedPaystackReferences() {
        if (refundedPaystackReferences == null) {
            refundedPaystackReferences = new ArrayList<>();
        }
        return refundedPaystackReferences;
    }
}
