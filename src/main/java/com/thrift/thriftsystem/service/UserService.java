package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.response.UserResponse;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
    public List<User> getAllUserModels() {
        return userRepository.findAll();
    }
}
