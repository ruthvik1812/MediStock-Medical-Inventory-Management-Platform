package com.medistock.medistock_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Security & Token Unit Tests")
class JwtServiceTest {

    private static final String TEST_SECRET = "medistock-super-secure-production-ready-jwt-secret-key-2026-minimum-256-bits";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
        jwtService.init();
    }

    @Test
    @DisplayName("Should successfully generate token and extract subject email")
    void shouldGenerateTokenAndExtractEmail() {
        String email = "pharmacist@medistock.com";
        Map<String, Object> claims = Map.of("role", "ROLE_PHARMACIST", "uid", 101L);

        String token = jwtService.generateToken(email, claims);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length, "JWT must consist of three parts (header.payload.signature)");

        String extracted = jwtService.extractEmail(token);
        assertEquals(email, extracted);
    }

    @Test
    @DisplayName("Should extract custom claims correctly")
    void shouldExtractCustomClaims() {
        String email = "admin@medistock.com";
        Map<String, Object> claims = Map.of("role", "ROLE_ADMIN", "uid", 1L);

        String token = jwtService.generateToken(email, claims);

        String role = jwtService.extractClaim(token, c -> c.get("role", String.class));
        assertEquals("ROLE_ADMIN", role);
    }

    @Test
    @DisplayName("Should validate token against expected email")
    void shouldValidateToken() {
        String email = "staff@medistock.com";
        String token = jwtService.generateToken(email, Map.of("role", "ROLE_STAFF"));

        assertTrue(jwtService.isTokenValid(token, email));
        assertTrue(jwtService.isTokenValid(token, "STAFF@MEDISTOCK.COM"), "Email match should be case-insensitive");
        assertFalse(jwtService.isTokenValid(token, "other@medistock.com"));
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.string", "test@medistock.com"));
        assertFalse(jwtService.isTokenValid("", "test@medistock.com"));
        assertFalse(jwtService.isTokenValid(null, "test@medistock.com"));
    }

    @Test
    @DisplayName("Should reject secret shorter than 32 bytes (256 bits)")
    void shouldRejectShortSecret() {
        JwtService insecureService = new JwtService("too-short-secret", EXPIRATION_MS);
        assertThrows(IllegalStateException.class, insecureService::init);
    }
}
