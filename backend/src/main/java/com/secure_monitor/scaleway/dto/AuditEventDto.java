package com.secure_monitor.scaleway.dto;

// AuditEventDto.java
// If using Jackson, add these annotations for snake_case JSON.

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AuditEventDto(
        String id,

        @JsonProperty("recorded_at")
        Instant recordedAt,

        String locality,

        PrincipalDto principal,

        @JsonProperty("organization_id")
        String organizationId,

        @JsonProperty("source_ip")
        String sourceIp,

        @JsonProperty("user_agent")
        String userAgent,

        @JsonProperty("product_name")
        String productName,

        @JsonProperty("service_name")
        String serviceName,

        @JsonProperty("method_name")
        String methodName,

        @JsonProperty("request_id")
        String requestId,

        @JsonProperty("request_body")
        Map<String, Object> requestBody,

        @JsonProperty("status_code")
        int statusCode,

        List<ResourceDto> resources
) {}