package com.medicalscheduling.service;

import com.medicalscheduling.domain.User;
import com.medicalscheduling.dto.request.LoginRequest;
import com.medicalscheduling.dto.request.RefreshTokenRequest;
import com.medicalscheduling.dto.request.RegisterRequest;
import com.medicalscheduling.dto.response.TokenResponse;
import com.medicalscheduling.dto.response.UserResponse;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }

        var user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                User.Role.PATIENT
        );

        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("User not found"));

        String accessToken = tokenService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = tokenService.generateRefreshToken(user.getEmail());

        return new TokenResponse(accessToken, refreshToken, tokenService.getAccessTokenExpiration());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        if (!tokenService.validateToken(request.refreshToken())) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        String email = tokenService.getEmailFromToken(request.refreshToken());
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        String accessToken = tokenService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = tokenService.generateRefreshToken(user.getEmail());

        return new TokenResponse(accessToken, refreshToken, tokenService.getAccessTokenExpiration());
    }
}
