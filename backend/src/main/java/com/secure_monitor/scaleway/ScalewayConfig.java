package com.secure_monitor.scaleway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "scaleway")
public record ScalewayConfig(
        String secretKey,
        String accessKey,
        String organizationId,
        String region,
        @DefaultValue("P7D") Duration eventLookback
) {
}
