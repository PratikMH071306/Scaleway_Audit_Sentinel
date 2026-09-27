package com.secure_monitor.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;



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

    private int statusCode;

    // resources/raw payload later
}