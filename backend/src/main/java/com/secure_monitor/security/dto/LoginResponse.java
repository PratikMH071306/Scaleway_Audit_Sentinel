package com.secure_monitor.security.dto;

import com.secure_monitor.security.Role;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        Role role
) {
}
