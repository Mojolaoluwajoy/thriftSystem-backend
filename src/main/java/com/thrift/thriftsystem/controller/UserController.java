package com.thrift.thriftsystem.controller;

import com.thrift.thriftsystem.dto.request.BankAccountRequest;
import com.thrift.thriftsystem.dto.response.ApiResponse;
import com.thrift.thriftsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getUserProfile() {
        return new ResponseEntity<>(
                ApiResponse.success("Profile found", userService.getCurrentUserProfile()),
                HttpStatus.OK);
    }

    @GetMapping("/id")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable String id) {
        return new ResponseEntity<>(
                ApiResponse.success("User retrieved", userService.getUserById(id)), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> getAllUsers() {
        return new ResponseEntity<>(
                ApiResponse.success("All found", userService.getAllUsers()), HttpStatus.OK);
    }

    @PatchMapping("me/whatsapp")
    public ResponseEntity<ApiResponse> updateWhatsappNumber(@RequestParam String whatsappNumber) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "whatsapp number updated",
                        userService.updateWhatsappNumber(whatsappNumber)),
                HttpStatus.OK);
    }

    @PutMapping("me/bank-account")
    public ResponseEntity<ApiResponse> updateBankAccount(
            @Valid @RequestBody BankAccountRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success("bank account updated", userService.updateBankAccount(request)),
                HttpStatus.OK);
    }

    @PatchMapping("me/profile-picture")
    public ResponseEntity<ApiResponse> updateProfilePicture(
            @RequestParam String profilePictureUrl) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "profile picture updated",
                        userService.updateProfilePicture(profilePictureUrl)),
                HttpStatus.OK);
    }
}
