package com.secure_monitor.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-secret-key-32-bytes!!";

    @Test
    void tokenCarriesUsernameAndRoleWithoutSecrets() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 3600));

        String token = jwtService.generateToken("pratik", Role.ADMIN);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("pratik");
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.ADMIN);
        assertThat(token).doesNotContain("password");
        assertThat(token).doesNotContain("scaleway");
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, -60));

        String token = jwtService.generateToken("pratik", Role.USER);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        JwtService issuer = new JwtService(new JwtProperties(SECRET, 3600));
        JwtService verifier = new JwtService(new JwtProperties("another-test-secret-key-32-bytes!!", 3600));

        String token = issuer.generateToken("pratik", Role.USER);

        assertThat(verifier.isTokenValid(token)).isFalse();
    }

    @Test
    void refusesAShortSigningSecret() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 3600)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
