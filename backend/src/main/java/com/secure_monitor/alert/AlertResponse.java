package com.secure_monitor.alert;

import com.secure_monitor.domain.AlertEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        String rule,
        AlertSeverity severity,
        AlertStatus status,
        String title,
        String description,
        Instant detectedAt,
        String sourceIp,
        String organizationId,
        String username,
        String scalewayUserId,
        Boolean scalewayUserLocked,
        int eventCount,
        List<String> relatedEventIds
) {
    public static AlertResponse from(AlertEntity entity) {
        return new AlertResponse(
                entity.getId(),
                entity.getRule(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDetectedAt(),
                entity.getSourceIp(),
                entity.getOrganizationId(),
                entity.getUsername(),
                entity.getScalewayUserId(),
                entity.getScalewayUserLocked(),
                entity.getEventCount(),
                entity.getRelatedEventIds()
        );
    }
}
