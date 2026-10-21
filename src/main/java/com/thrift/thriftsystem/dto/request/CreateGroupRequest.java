package com.thrift.thriftsystem.dto.request;

import com.thrift.thriftsystem.enums.ContributionFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    private String description;

    @NotNull(message = "Contribution amount is required")
    @Positive(message = "Contribution amount must be positive")
    private BigDecimal contributionAmount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Frequency is required")
    private ContributionFrequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Total members is required")
    @Positive(message = "Total members must be positive")
    private Integer totalMembers;
}
