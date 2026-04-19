package com.medicalscheduling.service;

import com.medicalscheduling.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci1tZWRpY2FsLXNjaGVkdWxpbmctYXBpLWp3dC10b2tlbi1zaWduaW5n");
        jwtConfig.setAccessTokenExpiration(7200000);
        jwtConfig.setRefreshTokenExpiration(604800000);
        tokenService = new TokenService(jwtConfig);
    }

    @Test
    void shouldGenerateAccessToken() {
        String token = tokenService.generateAccessToken("test@email.com", "PATIENT");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldGenerateRefreshToken() {
        String token = tokenService.generateRefreshToken("test@email.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldValidateValidToken() {
        String token = tokenService.generateAccessToken("test@email.com", "PATIENT");
        assertTrue(tokenService.validateToken(token));
    }

    @Test
    void shouldInvalidateInvalidToken() {
        assertFalse(tokenService.validateToken("invalid.token.here"));
    }

    @Test
    void shouldExtractEmailFromToken() {
        String email = "test@email.com";
        String token = tokenService.generateAccessToken(email, "PATIENT");
        assertEquals(email, tokenService.getEmailFromToken(token));
    }

    @Test
    void shouldExtractRoleFromAccessToken() {
        String token = tokenService.generateAccessToken("test@email.com", "ADMIN");
        assertEquals("ADMIN", tokenService.getRoleFromToken(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtConfig shortConfig = new JwtConfig();
        shortConfig.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci1tZWRpY2FsLXNjaGVkdWxpbmctYXBpLWp3dC10b2tlbi1zaWduaW5n");
        shortConfig.setAccessTokenExpiration(0);
        shortConfig.setRefreshTokenExpiration(0);
        TokenService shortTokenService = new TokenService(shortConfig);

        String token = shortTokenService.generateAccessToken("test@email.com", "PATIENT");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(shortTokenService.validateToken(token));
    }

    @Test
    void shouldReturnAccessTokenExpiration() {
        assertEquals(7200000, tokenService.getAccessTokenExpiration());
    }
}
