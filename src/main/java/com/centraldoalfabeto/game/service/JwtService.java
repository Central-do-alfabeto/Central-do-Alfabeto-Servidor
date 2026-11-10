package com.centraldoalfabeto.game.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private static final int MIN_SECRET_LENGTH = 32;

    private final String secret;
    private final long expirationMs;
    private SecretKey secretKey;

    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    void init() {
        this.secretKey = buildSecretKey(secret);
    }

    public String generateToken(UUID userId, String role, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId)) 
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role != null ? role.toUpperCase() : null)
                .claim("email", email)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey buildSecretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "Variável de ambiente JWT_SECRET não definida. Defina um segredo aleatório com pelo menos "
                    + MIN_SECRET_LENGTH + " caracteres.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT_SECRET é curto demais: são necessários pelo menos " + MIN_SECRET_LENGTH
                    + " caracteres (256 bits).");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
