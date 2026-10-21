package com.thrift.thriftsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContributionRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotNull(message = "Cycle number is required")
    private Integer cycleNumber;

    @NotBlank(message = "Currency is required")
    private String currency;
}
