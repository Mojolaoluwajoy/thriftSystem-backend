package com.thrift.thriftsystem.model;

import com.thrift.thriftsystem.enums.ContributionFrequency;
import com.thrift.thriftsystem.enums.GroupStatus;
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
@Document(collection = "thrift_groups")
public class ThriftGroup {

    @Id private String id;

    private String name;
    private String description;
    private String adminId;

    private BigDecimal contributionAmount;
    private String currency;

    @Builder.Default private GroupStatus status = GroupStatus.ACTIVE;

    private Integer totalMember;
    private Integer currentCycle;
    private Integer totalCycles;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private ContributionFrequency frequency;

    @Builder.Default private List<String> memberIds = new ArrayList<>();

    @CreatedDate private LocalDateTime createdDate;

    @LastModifiedDate private LocalDateTime lastModifiedDate;
}
