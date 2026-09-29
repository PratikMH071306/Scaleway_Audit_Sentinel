package com.secure_monitor.remediation;

import com.secure_monitor.domain.RemediationEntity;

import java.util.UUID;

public record RemediationResponse(
        UUID remediationId,
        UUID alertId,
        String action,
        String targetUserId,
        String targetUsername,
        String status,
        boolean beforeLocked,
        boolean afterLocked,
        String message
) {
    public static RemediationResponse from(RemediationEntity entity) {
        return new RemediationResponse(
                entity.getId(),
                entity.getAlertId(),
                entity.getAction().name(),
                entity.getTargetUserId(),
                entity.getTargetUsername(),
                entity.getStatus().name(),
                entity.isBeforeLocked(),
                entity.isAfterLocked(),
                entity.getMessage()
        );
    }
}
