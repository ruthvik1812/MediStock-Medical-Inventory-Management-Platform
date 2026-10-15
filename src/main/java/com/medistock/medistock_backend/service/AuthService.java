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
import com.medistock.medistock_backend.util.RoleConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expirationMs = expirationMs;
    }

    public LoginResponse login(LoginRequest req) {
        // 1. Authenticate — throws BadCredentialsException on failure,
        //    which GlobalExceptionHandler converts to a 401 with a generic message.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        // 2. auth.getPrincipal() is our CustomUserDetails
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        User user = principal.getUser();

        // 3. Extract the primary role for the JWT payload.
        String primaryRole = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse(RoleConstants.STAFF);

        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of("role", primaryRole, "uid", user.getId())
        );

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(expirationMs)
                .user(toUserResponse(user))
                .build();
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        final String resolvedRoleName;
        if (req.getRole() != null && !req.getRole().isBlank()) {
            String candidate = req.getRole().trim().toUpperCase().replace("ROLE_", "");
            if (candidate.equals(RoleConstants.ADMIN) || candidate.equals(RoleConstants.PHARMACIST)
                    || candidate.equals(RoleConstants.STAFF) || candidate.equals(RoleConstants.SUPPLIER)) {
                resolvedRoleName = candidate;
            } else {
                resolvedRoleName = RoleConstants.PHARMACIST;
            }
        } else {
            resolvedRoleName = RoleConstants.PHARMACIST;
        }

        Role role = roleRepository.findByName(resolvedRoleName)
                .orElseGet(() -> roleRepository.save(new Role(null, resolvedRoleName, resolvedRoleName + " user")));

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(req.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone() != null ? req.getPhone().trim() : null)
                .enabled(true)
                .roles(Set.of(role))
                .build();

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(
                saved.getEmail(),
                Map.of("role", role.getName(), "uid", saved.getId())
        );

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(expirationMs)
                .user(toUserResponse(saved))
                .build();
    }

    public UserResponse currentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .enabled(u.isEnabled())
                .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}