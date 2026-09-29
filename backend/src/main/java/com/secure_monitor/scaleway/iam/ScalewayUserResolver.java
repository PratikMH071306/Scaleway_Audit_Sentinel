package com.secure_monitor.scaleway.iam;

import com.secure_monitor.domain.AlertEntity;
import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.AlertRepository;
import com.secure_monitor.repository.SecurityEventRepository;
import com.secure_monitor.scaleway.dto.ScalewayUserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ScalewayUserResolver {

    private static final Logger log = LoggerFactory.getLogger(ScalewayUserResolver.class);

    private final SecurityEventRepository events;
    private final AlertRepository alerts;
    private final ScalewayIamClient iamClient;

    public ScalewayUserResolver(
            SecurityEventRepository events,
            AlertRepository alerts,
            ScalewayIamClient iamClient
    ) {
        this.events = events;
        this.alerts = alerts;
        this.iamClient = iamClient;
    }

    public AlertEntity attachUserId(AlertEntity alert) {
        if (alert.getScalewayUserId() != null && !alert.getScalewayUserId().isBlank()) {
            return alert;
        }
        String fromEvents = userIdFromEvents(alert.getRelatedEventIds());
        String userId = fromEvents != null ? fromEvents : userIdFromIam(alert);
        if (userId == null) {
            return alert;
        }
        alert.setScalewayUserId(userId);
        return alerts.save(alert);
    }

    public AlertEntity refreshLockState(AlertEntity alert) {
        if (alert.getScalewayUserId() == null || alert.getScalewayUserId().isBlank()) {
            return alert;
        }
        try {
            ScalewayUserDto user = iamClient.getUser(alert.getScalewayUserId());
            if (alert.getScalewayUserLocked() == null || alert.getScalewayUserLocked() != user.locked()) {
                alert.setScalewayUserLocked(user.locked());
                return alerts.save(alert);
            }
            return alert;
        } catch (ScalewayApiException exception) {
            log.warn("Could not read Scaleway lock state for {} : {}", alert.getScalewayUserId(), exception.getSafeMessage());
            return alert;
        }
    }

    private String userIdFromEvents(List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return null;
        }
        for (SecurityEventEntity event : events.findAllById(eventIds)) {
            if (event.getScalewayUserId() != null && !event.getScalewayUserId().isBlank()) {
                return event.getScalewayUserId();
            }
            String fromResource = iamUserId(event.getResources());
            if (fromResource != null) {
                return fromResource;
            }
        }
        return null;
    }

    private String userIdFromIam(AlertEntity alert) {
        if (alert.getUsername() == null || alert.getUsername().isBlank()) {
            return null;
        }
        if (alert.getOrganizationId() == null || alert.getOrganizationId().isBlank()) {
            return null;
        }
        try {
            return iamClient.listUsers(alert.getOrganizationId()).stream()
                    .filter(user -> alert.getUsername().equals(user.username()))
                    .map(ScalewayUserDto::id)
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (ScalewayApiException exception) {
            log.warn("Could not resolve Scaleway user {} : {}", alert.getUsername(), exception.getSafeMessage());
            return null;
        }
    }

    private static String iamUserId(List<Map<String, Object>> resources) {
        if (resources == null) {
            return null;
        }
        for (Map<String, Object> resource : resources) {
            if (resource == null || !"iam_user".equals(String.valueOf(resource.get("type")))) {
                continue;
            }
            Object id = resource.get("id");
            if (id == null || String.valueOf(id).isBlank()) {
                return null;
            }
            return String.valueOf(id);
        }
        return null;
    }
}
