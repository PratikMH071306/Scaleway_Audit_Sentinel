package com.secure_monitor.detection;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetectionResult(
        boolean detected,
        String rule,
        String description,
        int eventCount,
        String sourceIp
) {
}
