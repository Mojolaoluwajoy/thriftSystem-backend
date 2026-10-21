package com.thrift.thriftsystem.dto.response;


import com.thrift.thriftsystem.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private String whatsappNumber;
    private String bankAccountName;
    private boolean bankAccountLinked;
    private Role role;
    private Set<Role> roles;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
