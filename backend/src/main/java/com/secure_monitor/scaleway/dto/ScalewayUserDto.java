package com.secure_monitor.scaleway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScalewayUserDto(
        String id,
        String username,
        String email,
        @JsonProperty("organization_id") String organizationId,
        boolean locked,
        String type,
        @JsonProperty("last_login_at") Instant lastLoginAt
) {
}
