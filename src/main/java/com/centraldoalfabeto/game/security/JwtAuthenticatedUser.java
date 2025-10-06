package com.centraldoalfabeto.game.security;

import java.util.UUID;

public class JwtAuthenticatedUser {
    private final UUID userId; 
    private final String role;
    private final String email;

    public JwtAuthenticatedUser(UUID userId, String role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }
}
