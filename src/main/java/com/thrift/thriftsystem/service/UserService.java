package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.request.BankAccountRequest;
import com.thrift.thriftsystem.dto.response.UserResponse;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaystackService paystackService;

    public User getCurrentUser(){
        String email= SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).
                orElseThrow(()-> new ResourceNotFoundException("User not found"));
    }
    public UserResponse getCurrentUserProfile(){
       return userMapper.toResponse(getCurrentUser());

    }

    public UserResponse getUserById(String id){
        User user = userRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("User not found with id: "+id));
        return userMapper.toResponse(user);

    }
    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateWhatsappNumber(String whatsappNumber){
        User user = getCurrentUser();

        user.setWhatsappNumber(whatsappNumber);
        userRepository.save(user);
        log.info("Whatsapp number updated for : {}", user.getEmail());
        return userMapper.toResponse(user);
    }
    public UserResponse updateProfilePicture(String profilePictureUrl){
        User user = getCurrentUser();
        user.setProfilePicture(profilePictureUrl);
        userRepository.save(user);
        log.info("Profile picture updated for : {}", user.getEmail());
        return userMapper.toResponse(user);
    }
    public UserResponse updateBankAccount(BankAccountRequest request){
        User user = getCurrentUser();

        Map<String, Object> response = paystackService.createTransferRecipient(
                request.getAccountName(), request.getAccountNumber(), request.getBankCode());
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null || data.get("recipient_code") == null) {
            throw new BadRequestException("Could not verify bank account");
        }

        user.setBankAccountNumber(request.getAccountNumber());
        user.setBankCode(request.getBankCode());
        user.setBankAccountName(request.getAccountName());
        user.setPaystackRecipientCode((String) data.get("recipient_code"));
        userRepository.save(user);
        log.info("Bank account updated for : {}", user.getEmail());
        return userMapper.toResponse(user);
    }

    public List<User> getAllUserModels() {
        return userRepository.findAll();
    }
}
