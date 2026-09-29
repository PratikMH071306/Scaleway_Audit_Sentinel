package com.secure_monitor.security;

import com.secure_monitor.domain.UserEntity;
import com.secure_monitor.repository.UserRepository;
import com.secure_monitor.security.dto.LoginRequest;
import com.secure_monitor.security.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository users;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(new JwtProperties("test-only-jwt-secret-key-32-bytes!!", 3600));
        authenticationService = new AuthenticationService(users, passwordEncoder, jwtService);
    }

    @Test
    void returnsABearerTokenForAnEnabledUser() {
        when(users.findByUsername("pratik")).thenReturn(Optional.of(
                new UserEntity("pratik", passwordEncoder.encode("correct-password"), Role.ADMIN, true)
        ));

        LoginResponse response = authenticationService.login(new LoginRequest("pratik", "correct-password"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.username()).isEqualTo("pratik");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.accessToken()).doesNotContain("correct-password");
        assertThat(jwtService.extractUsername(response.accessToken())).isEqualTo("pratik");
    }

    @Test
    void rejectsAWrongPassword() {
        when(users.findByUsername("pratik")).thenReturn(Optional.of(
                new UserEntity("pratik", passwordEncoder.encode("correct-password"), Role.USER, true)
        ));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("pratik", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(AuthenticationService.INVALID_CREDENTIALS);
    }

    @Test
    void rejectsAnUnknownUser() {
        when(users.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("missing", "correct-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(AuthenticationService.INVALID_CREDENTIALS);
    }

    @Test
    void rejectsADisabledUser() {
        when(users.findByUsername("pratik")).thenReturn(Optional.of(
                new UserEntity("pratik", passwordEncoder.encode("correct-password"), Role.USER, false)
        ));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("pratik", "correct-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(AuthenticationService.INVALID_CREDENTIALS);
    }
}
