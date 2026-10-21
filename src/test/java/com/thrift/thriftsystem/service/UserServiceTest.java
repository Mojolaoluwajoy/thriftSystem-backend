package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.dto.response.UserResponse;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.util.UserMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private UserMapper userMapper;

    @Mock private SecurityContext securityContext;

    @Mock private Authentication authentication;

    @InjectMocks private UserService userService;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user =
                User.builder()
                        .id("user123")
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .phoneNumber("08012345678")
                        .enabled(true)
                        .build();

        userResponse =
                UserResponse.builder()
                        .id("user123")
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .build();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john@example.com");
    }

    @Test
    void getCurrentUser_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        User result = userService.getCurrentUser();

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getCurrentUser_NotFound_ThrowsException() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUserProfile_Success() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.getCurrentUserProfile();

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        verify(userMapper, times(1)).toResponse(any());
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.getUserById("user123");

        assertNotNull(result);
        assertEquals("user123", result.getId());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById("invalid-id"));
    }

    @Test
    void getAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        List<UserResponse> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void updateWhatsappNumber_Success() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.updateWhatsappNumber("2348012345678");

        assertNotNull(result);
        verify(userRepository, times(1)).save(any());
    }
}
