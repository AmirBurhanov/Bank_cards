package org.example.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bank.dto.request.RegisterRequest;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.Role;
import org.example.bank.exception.EmailAlreadyRegisteredException;
import org.example.bank.exception.UsernameAlreadyExistsException;
import org.example.bank.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public User register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already taken");  // ← ИСПРАВЛЕНО
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException("Email already registered");  // ← ИСПРАВЛЕНО
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: {}", request.getUsername());

        return savedUser;
    }

    public Authentication authenticate(String username, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
    }
}