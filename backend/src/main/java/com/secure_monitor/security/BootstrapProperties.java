package com.secure_monitor.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.bootstrap")
public record BootstrapProperties(String username, String password) {
}
