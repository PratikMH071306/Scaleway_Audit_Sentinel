package com.secure_monitor.security;

import com.secure_monitor.domain.UserEntity;
import com.secure_monitor.repository.UserRepository;
import com.secure_monitor.security.dto.LoginRequest;
import com.secure_monitor.security.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationService {

    static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public AuthenticationService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("unused-timing-placeholder");
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = users.findByUsername(request.username()).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !passwordMatches || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(), user.getUsername(), user.getRole());
    }
}
