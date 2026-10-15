package com.medistock.medistock_backend.dto.response;

import java.util.HashSet;
import java.util.Set;

/**
 * Public-safe representation of a user.
 * Note: no password hash, no internal-only fields.
 */
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private boolean enabled;
    private Set<String> roles = new HashSet<>();

    public UserResponse() {}

    public UserResponse(Long id, String fullName, String email, String phone, boolean enabled, Set<String> roles) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.enabled = enabled;
        this.roles = roles != null ? roles : new HashSet<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private boolean enabled;
        private Set<String> roles = new HashSet<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder roles(Set<String> roles) { this.roles = roles != null ? roles : new HashSet<>(); return this; }

        public UserResponse build() {
            return new UserResponse(id, fullName, email, phone, enabled, roles);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}