package com.secure_monitor.detection;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetectionResult(
        boolean detected,
        String rule,
        String description,
        int eventCount,
        String sourceIp,
        Instant detectedAt,
        List<String> relatedEventIds,
        String organizationId,
        String username,
        boolean followedBySuccess,
        String scalewayUserId
) {
}
