package com.secure_monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "secure-monitor.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
