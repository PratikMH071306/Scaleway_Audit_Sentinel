package com.secure_monitor.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthenticationContext {

    private AuthenticationContext() {
    }

    public static String currentUsername() {
        Authentication authentication = currentAuthentication();
        return authentication.getName();
    }

    public static UUID currentUserId() {
        Authentication authentication = currentAuthentication();
        if (authentication.getPrincipal() instanceof MonitorUserDetails details) {
            return details.getId();
        }
        return null;
    }

    public static Role currentRole() {
        Authentication authentication = currentAuthentication();
        if (authentication.getPrincipal() instanceof MonitorUserDetails details) {
            return details.getRole();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));
    }

    private static Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        return authentication;
    }
}
