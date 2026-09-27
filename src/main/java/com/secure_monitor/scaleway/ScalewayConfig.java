package com.secure_monitor.scaleway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scaleway")
public record ScalewayConfig(
        String secretKey,
        String accessKey,
        String organizationId,
        String region
) {
}
