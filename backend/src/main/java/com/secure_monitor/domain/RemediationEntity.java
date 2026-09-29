package com.secure_monitor.domain;

import com.secure_monitor.remediation.RemediationAction;
import com.secure_monitor.remediation.RemediationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "remediations")
public class RemediationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID alertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemediationAction action;

    @Column(nullable = false)
    private String targetUserId;

    private String targetUsername;

    private UUID performedByUserId;

    private String performedByUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemediationStatus status;

    private boolean beforeLocked;

    private boolean afterLocked;

    private String beforeState;

    private String afterState;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    private String errorMessage;

    private String message;

    protected RemediationEntity() {
    }

    public RemediationEntity(
            UUID alertId,
            RemediationAction action,
            String targetUserId,
            String targetUsername,
            UUID performedByUserId,
            String performedByUsername,
            boolean beforeLocked,
            Instant createdAt
    ) {
        this.alertId = alertId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.performedByUserId = performedByUserId;
        this.performedByUsername = performedByUsername;
        this.status = RemediationStatus.PENDING;
        this.beforeLocked = beforeLocked;
        this.beforeState = beforeLocked ? "locked" : "unlocked";
        this.createdAt = createdAt;
    }

    public void succeed(boolean afterLocked, String message, Instant completedAt) {
        this.status = RemediationStatus.SUCCESS;
        this.afterLocked = afterLocked;
        this.afterState = afterLocked ? "locked" : "unlocked";
        this.message = message;
        this.completedAt = completedAt;
        this.errorMessage = null;
    }

    public void fail(String message, Instant completedAt) {
        this.status = RemediationStatus.FAILED;
        this.afterLocked = beforeLocked;
        this.afterState = beforeState;
        this.message = message;
        this.errorMessage = message;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public RemediationAction getAction() {
        return action;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public UUID getPerformedByUserId() {
        return performedByUserId;
    }

    public String getPerformedByUsername() {
        return performedByUsername;
    }

    public RemediationStatus getStatus() {
        return status;
    }

    public boolean isBeforeLocked() {
        return beforeLocked;
    }

    public boolean isAfterLocked() {
        return afterLocked;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMessage() {
        return message;
    }
}
