package com.thrift.thriftsystem.security;

import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.UserRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(
                        user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority(role.name()))
                                .collect(Collectors.toList()))
                .build();
    }
}
