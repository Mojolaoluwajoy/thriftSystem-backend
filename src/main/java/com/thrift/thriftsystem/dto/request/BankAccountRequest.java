package com.thrift.thriftsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BankAccountRequest {

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{10}", message = "Account number must be 10 digits")
    private String accountNumber;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Account name is required")
    private String accountName;
}
