package com.secure_monitor.scaleway.dto;

import java.util.List;

public record AuditEventsResponse(
        List<AuditEventDto> events
) {}
