package com.secure_monitor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    static final String ROLE_CLAIM = "role";
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, Role role) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusSeconds(properties.expiration());
        return Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public Role extractRole(String token) {
        String role = claims(token).get(ROLE_CLAIM, String.class);
        return Role.valueOf(role);
    }

    public boolean isTokenValid(String token) {
        try {
            claims(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public long expiresInSeconds() {
        return properties.expiration();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(0)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
