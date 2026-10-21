package com.thrift.thriftsystem.util;

import com.thrift.thriftsystem.dto.request.RegisterRequest;
import com.thrift.thriftsystem.dto.response.AuthResponse;
import com.thrift.thriftsystem.dto.response.UserResponse;
import com.thrift.thriftsystem.enums.Role;
import com.thrift.thriftsystem.model.User;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserMapper {

    public User toModel(RegisterRequest request,String encodedPassword) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(encodedPassword)
                .roles(Set.of(Role.ROLE_MEMBER))
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePicture(user.getProfilePicture())
                .whatsappNumber(user.getWhatsappNumber())
                .bankAccountName(user.getBankAccountName())
                .bankAccountLinked(user.getPaystackRecipientCode() != null)
                .roles(user.getRoles())
                .enabled(true)
                .emailVerified(true)
                .createdAt(user.getCreatedAt())
                .build();
    }
    public AuthResponse toAuthResponse(User user,String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();

    }
}
