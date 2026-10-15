package com.medistock.medistock_backend.service;

import com.medistock.medistock_backend.dto.request.LoginRequest;
import com.medistock.medistock_backend.dto.request.RegisterRequest;
import com.medistock.medistock_backend.dto.response.LoginResponse;
import com.medistock.medistock_backend.dto.response.UserResponse;
import com.medistock.medistock_backend.entity.Role;
import com.medistock.medistock_backend.entity.User;
import com.medistock.medistock_backend.exception.ApiException;
import com.medistock.medistock_backend.repository.RoleRepository;
import com.medistock.medistock_backend.repository.UserRepository;
import com.medistock.medistock_backend.security.CustomUserDetails;
import com.medistock.medistock_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    private AuthService authService;

    private static final long EXPIRATION_MS = 900000;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                userRepository,
                roleRepository,
                passwordEncoder,
                jwtService,
                EXPIRATION_MS
        );
    }

    @Test
    @DisplayName("Should successfully authenticate user and return JWT with profile")
    void login_Success() {
        Role role = Role.builder().id(1L).name("PHARMACIST").description("Pharmacist").build();
        User user = User.builder()
                .id(10L)
                .fullName("Dr. Sarah Mitchell")
                .email("sarah@medistock.com")
                .passwordHash("hashedPassword")
                .roles(Set.of(role))
                .enabled(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(eq("sarah@medistock.com"), any())).thenReturn("mocked.jwt.token");

        LoginRequest request = new LoginRequest("sarah@medistock.com", "Password!123");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(EXPIRATION_MS, response.getExpiresInMs());
        assertEquals("Dr. Sarah Mitchell", response.getUser().getFullName());
        assertTrue(response.getUser().getRoles().contains("PHARMACIST"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(eq("sarah@medistock.com"), any());
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException on authentication failure")
    void login_BadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        LoginRequest request = new LoginRequest("sarah@medistock.com", "WrongPassword");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Should register new user, encode password, and return JWT token")
    void register_Success() {
        RegisterRequest request = new RegisterRequest(
                "Elena Rostova",
                "nurse@medistock.com",
                "SecurePassword!123",
                "STAFF",
                "555-0192"
        );

        Role role = Role.builder().id(2L).name("STAFF").description("Staff user").build();
        User savedUser = User.builder()
                .id(20L)
                .fullName("Elena Rostova")
                .email("nurse@medistock.com")
                .passwordHash("bcrypt_encoded_hash")
                .phone("555-0192")
                .enabled(true)
                .roles(Set.of(role))
                .build();

        when(userRepository.existsByEmailIgnoreCase("nurse@medistock.com")).thenReturn(false);
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("SecurePassword!123")).thenReturn("bcrypt_encoded_hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(eq("nurse@medistock.com"), any())).thenReturn("nurse.jwt.token");

        LoginResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("nurse.jwt.token", response.getToken());
        assertEquals("Elena Rostova", response.getUser().getFullName());
        assertEquals("nurse@medistock.com", response.getUser().getEmail());
        assertTrue(response.getUser().getRoles().contains("STAFF"));
    }

    @Test
    @DisplayName("Should throw ApiException 409 Conflict when registering duplicate email")
    void register_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Duplicate",
                "existing@medistock.com",
                "Password!123",
                "PHARMACIST",
                null
        );

        when(userRepository.existsByEmailIgnoreCase("existing@medistock.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return user profile for existing email in currentUser")
    void currentUser_Success() {
        Role role = Role.builder().id(1L).name("ADMIN").build();
        User user = User.builder()
                .id(1L)
                .fullName("System Admin")
                .email("admin@medistock.com")
                .roles(Set.of(role))
                .enabled(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("admin@medistock.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.currentUser("admin@medistock.com");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("System Admin", response.getFullName());
        assertEquals("admin@medistock.com", response.getEmail());
    }

    @Test
    @DisplayName("Should throw ApiException 404 when user is not found in currentUser")
    void currentUser_NotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@medistock.com")).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> authService.currentUser("missing@medistock.com"));
    }
}
