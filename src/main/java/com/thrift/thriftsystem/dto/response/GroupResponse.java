package com.thrift.thriftsystem.dto.response;

import com.thrift.thriftsystem.enums.ContributionFrequency;
import com.thrift.thriftsystem.enums.GroupStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    private String id;
    private String name;
    private String description;
    private String adminId;
    private String adminName;
    private BigDecimal contributionAmount;
    private String currency;
    private GroupStatus status;
    private Integer totalMembers;
    private Integer currentCycle;
    private Integer totalCycles;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime startedAt;
    private ContributionFrequency frequency;
    private List<String> memberIds;
    private List<String> memberNames;
    private LocalDateTime createdAt;
}
