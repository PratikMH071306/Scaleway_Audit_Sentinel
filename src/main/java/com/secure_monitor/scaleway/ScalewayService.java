package com.secure_monitor.scaleway;

import com.secure_monitor.scaleway.dto.AuditEventsResponse;
import org.springframework.stereotype.Service;

@Service
public class ScalewayService {

    private final ScalewayClient client;

    public ScalewayService(ScalewayClient client) {
        this.client = client;
    }

    public AuditEventsResponse getAuditEvents() {
        return client.getEvents();
    }
}
