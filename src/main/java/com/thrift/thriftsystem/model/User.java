package com.thrift.thriftsystem.model;

import com.thrift.thriftsystem.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String email;
    private String phoneNumber;
    private String whatsappNumber;

    private String firstName;
    private String lastName;
    private String password;
    private String profilePicture;
    private String bankAccountNumber;
    private String bankCode;
    private String bankAccountName;
    private String paystackRecipientCode;

    @Builder.Default
    private Set<Role> roles=new HashSet<>();

    @Builder.Default
    private boolean enabled=true;

    @Builder.Default
    private boolean emailVerified=false;

    private String resetPasswordToken;
    private LocalDateTime resetPasswordExpiry;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


}
