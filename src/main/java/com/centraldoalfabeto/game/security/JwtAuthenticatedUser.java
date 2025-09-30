package com.centraldoalfabeto.game.security;

public class JwtAuthenticatedUser {
    private final Long userId;
    private final String role;
    private final String email;

    public JwtAuthenticatedUser(Long userId, String role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }
}
