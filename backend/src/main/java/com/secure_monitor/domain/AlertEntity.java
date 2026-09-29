package com.secure_monitor.domain;

import com.secure_monitor.alert.AlertSeverity;
import com.secure_monitor.alert.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String rule;

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private String title;

    private String description;

    private Instant detectedAt;

    private String sourceIp;

    private String organizationId;

    private String username;

    private String scalewayUserId;

    private Boolean scalewayUserLocked;

    private int eventCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_event_ids")
    private List<String> relatedEventIds;

    protected AlertEntity() {
    }

    public AlertEntity(
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
            int eventCount,
            List<String> relatedEventIds
    ) {
        this.rule = rule;
        this.severity = severity;
        this.status = status;
        this.title = title;
        this.description = description;
        this.detectedAt = detectedAt;
        this.sourceIp = sourceIp;
        this.organizationId = organizationId;
        this.username = username;
        this.scalewayUserId = scalewayUserId;
        this.eventCount = eventCount;
        this.relatedEventIds = relatedEventIds;
    }

    public UUID getId() {
        return id;
    }

    public String getRule() {
        return rule;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getUsername() {
        return username;
    }

    public String getScalewayUserId() {
        return scalewayUserId;
    }

    public void setScalewayUserId(String scalewayUserId) {
        this.scalewayUserId = scalewayUserId;
    }

    public Boolean getScalewayUserLocked() {
        return scalewayUserLocked;
    }

    public void setScalewayUserLocked(Boolean scalewayUserLocked) {
        this.scalewayUserLocked = scalewayUserLocked;
    }

    public int getEventCount() {
        return eventCount;
    }

    public List<String> getRelatedEventIds() {
        return relatedEventIds;
    }
}
