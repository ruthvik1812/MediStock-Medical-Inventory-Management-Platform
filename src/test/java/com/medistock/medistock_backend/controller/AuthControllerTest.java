package com.medistock.medistock_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medistock.medistock_backend.dto.request.LoginRequest;
import com.medistock.medistock_backend.dto.response.LoginResponse;
import com.medistock.medistock_backend.dto.response.UserResponse;
import com.medistock.medistock_backend.exception.GlobalExceptionHandler;
import com.medistock.medistock_backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Web/REST Endpoint Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/login - 200 OK on valid credentials")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("pharmacist@medistock.com");
        request.setPassword("SecurePass123!");

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .fullName("Elena Rostova")
                .email("pharmacist@medistock.com")
                .roles(Set.of("ROLE_PHARMACIST"))
                .enabled(true)
                .build();

        LoginResponse loginResponse = LoginResponse.builder()
                .token("mocked.jwt.token.sample")
                .tokenType("Bearer")
                .expiresInMs(900000)
                .user(userResponse)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token.sample"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("pharmacist@medistock.com"))
                .andExpect(jsonPath("$.user.fullName").value("Elena Rostova"));
    }

    @Test
    @DisplayName("POST /api/auth/register - 201 Created on valid registration")
    void register_Success() throws Exception {
        com.medistock.medistock_backend.dto.request.RegisterRequest request =
                new com.medistock.medistock_backend.dto.request.RegisterRequest(
                        "Dr. Sarah Mitchell",
                        "sarah@medistock.com",
                        "Password!123",
                        "PHARMACIST",
                        "555-1234"
                );

        UserResponse userResponse = UserResponse.builder()
                .id(5L)
                .fullName("Dr. Sarah Mitchell")
                .email("sarah@medistock.com")
                .roles(Set.of("PHARMACIST"))
                .enabled(true)
                .build();

        LoginResponse loginResponse = LoginResponse.builder()
                .token("mocked.jwt.registered.token")
                .tokenType("Bearer")
                .expiresInMs(900000)
                .user(userResponse)
                .build();

        when(authService.register(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mocked.jwt.registered.token"))
                .andExpect(jsonPath("$.user.email").value("sarah@medistock.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 400 Bad Request on invalid email or blank password")
    void login_ValidationError() throws Exception {
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setEmail("not-an-email");
        invalidRequest.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - 401 Unauthorized on invalid credentials")
    void login_BadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@medistock.com");
        request.setPassword("WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("GET /api/auth/me - 200 OK returns user profile for authenticated principal")
    void me_Success() throws Exception {
        UserDetails principal = new User("admin@medistock.com", "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        UserResponse userResponse = UserResponse.builder()
                .id(2L)
                .fullName("System Administrator")
                .email("admin@medistock.com")
                .roles(Set.of("ROLE_ADMIN"))
                .enabled(true)
                .build();

        when(authService.currentUser("admin@medistock.com")).thenReturn(userResponse);

        try {
            mockMvc.perform(get("/api/auth/me")
                            .principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("admin@medistock.com"))
                    .andExpect(jsonPath("$.fullName").value("System Administrator"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
