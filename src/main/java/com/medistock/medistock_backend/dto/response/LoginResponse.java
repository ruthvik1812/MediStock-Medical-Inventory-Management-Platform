package com.medistock.medistock_backend.dto.response;

public class LoginResponse {
    private String token;
    private String tokenType;   // always "Bearer"
    private long expiresInMs;
    private UserResponse user;

    public LoginResponse() {}

    public LoginResponse(String token, String tokenType, long expiresInMs, UserResponse user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType = "Bearer";
        private long expiresInMs;
        private UserResponse user;

        public Builder token(String token) { this.token = token; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder expiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; return this; }
        public Builder user(UserResponse user) { this.user = user; return this; }

        public LoginResponse build() {
            return new LoginResponse(token, tokenType, expiresInMs, user);
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
}