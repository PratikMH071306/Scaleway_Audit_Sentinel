package com.secure_monitor.domain;

import java.time.Instant;

public record SecurityEvent(
        String eventId,
        Instant timestamp,
        String principalId,
        String principalType,
        String sourceIp,
        String action,
        String resource,
        String status
) {
}
