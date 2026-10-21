package com.thrift.thriftsystem.model;

import com.thrift.thriftsystem.enums.MembershipStatus;
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
@Document(collection = "memberships")
public class Membership {

    @Id private String id;

    private String userId;
    private String groupId;
    private Integer payoutOrder;

    @Builder.Default private MembershipStatus status = MembershipStatus.ACTIVE;

    @Builder.Default private boolean hasPaidCurrentCycle = false;

    @Builder.Default private boolean hasCollected = false;

    private LocalDateTime joinedAt;
    private LocalDateTime collectionDate;

    @CreatedDate private LocalDateTime createdDate;

    @CreatedDate private LocalDateTime updatedDate;
}
