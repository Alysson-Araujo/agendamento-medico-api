package com.medicalscheduling.service;

import com.medicalscheduling.domain.User;
import com.medicalscheduling.dto.request.LoginRequest;
import com.medicalscheduling.dto.request.RefreshTokenRequest;
import com.medicalscheduling.dto.request.RegisterRequest;
import com.medicalscheduling.exception.BusinessException;
import com.medicalscheduling.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User patientUser;

    @BeforeEach
    void setUp() {
        patientUser = new User("Patient Name", "patient@email.com", "encodedPassword", User.Role.PATIENT);
        patientUser.setId(UUID.randomUUID());
    }

    @Test
    void shouldRegisterSuccessfully() {
        var request = new RegisterRequest("New Patient", "new@email.com", "password123");

        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        var savedUser = new User("New Patient", "new@email.com", "encodedPassword", User.Role.PATIENT);
        savedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = authService.register(request);

        assertNotNull(response);
        assertEquals("New Patient", response.name());
        assertEquals("new@email.com", response.email());
        assertEquals("PATIENT", response.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyRegistered() {
        var request = new RegisterRequest("Patient", "existing@email.com", "password123");

        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {
        var request = new LoginRequest("patient@email.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("patient@email.com")).thenReturn(Optional.of(patientUser));
        when(tokenService.generateAccessToken("patient@email.com", "PATIENT")).thenReturn("access-token");
        when(tokenService.generateRefreshToken("patient@email.com")).thenReturn("refresh-token");
        when(tokenService.getAccessTokenExpiration()).thenReturn(7200000L);

        var response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(7200000L, response.expiresIn());
    }

    @Test
    void shouldThrowWhenInvalidCredentials() {
        var request = new LoginRequest("patient@email.com", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void shouldThrowWhenUserNotFoundOnLogin() {
        var request = new LoginRequest("nonexistent@email.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("nonexistent@email.com")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        var request = new RefreshTokenRequest("valid-refresh-token");

        when(tokenService.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenService.getEmailFromToken("valid-refresh-token")).thenReturn("patient@email.com");
        when(userRepository.findByEmail("patient@email.com")).thenReturn(Optional.of(patientUser));
        when(tokenService.generateAccessToken("patient@email.com", "PATIENT")).thenReturn("new-access-token");
        when(tokenService.generateRefreshToken("patient@email.com")).thenReturn("new-refresh-token");
        when(tokenService.getAccessTokenExpiration()).thenReturn(7200000L);

        var response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
    }

    @Test
    void shouldThrowWhenRefreshTokenInvalid() {
        var request = new RefreshTokenRequest("invalid-token");

        when(tokenService.validateToken("invalid-token")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.refresh(request));
    }

    @Test
    void shouldThrowWhenUserNotFoundOnRefresh() {
        var request = new RefreshTokenRequest("valid-refresh-token");

        when(tokenService.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenService.getEmailFromToken("valid-refresh-token")).thenReturn("deleted@email.com");
        when(userRepository.findByEmail("deleted@email.com")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.refresh(request));
    }
}
