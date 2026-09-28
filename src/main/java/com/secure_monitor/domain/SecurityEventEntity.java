package com.secure_monitor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "security_events")
public class SecurityEventEntity {

    @Id
    private String eventId;

    private Instant recordedAt;

    private String locality;

    private String organizationId;

    private String sourceIp;

    private String userAgent;

    private String productName;

    private String serviceName;

    private String methodName;

    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body")
    private Map<String, Object> requestBody;

    private int statusCode;

    protected SecurityEventEntity() {
    }

    public SecurityEventEntity(
            String eventId,
            Instant recordedAt,
            String locality,
            String organizationId,
            String sourceIp,
            String userAgent,
            String productName,
            String serviceName,
            String methodName,
            String requestId,
            Map<String, Object> requestBody,
            int statusCode
    ) {
        this.eventId = eventId;
        this.recordedAt = recordedAt;
        this.locality = locality;
        this.organizationId = organizationId;
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
        this.productName = productName;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.requestId = requestId;
        this.requestBody = requestBody;
        this.statusCode = statusCode;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getLocality() {
        return locality;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getProductName() {
        return productName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getRequestId() {
        return requestId;
    }

    public Map<String, Object> getRequestBody() {
        return requestBody;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
