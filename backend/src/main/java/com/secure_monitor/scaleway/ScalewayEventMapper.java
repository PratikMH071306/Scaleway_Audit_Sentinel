package com.secure_monitor.scaleway;

import com.secure_monitor.domain.SecurityEvent;
import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.scaleway.dto.AuditEventDto;
import com.secure_monitor.scaleway.dto.ResourceDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ScalewayEventMapper {

    public SecurityEvent map(AuditEventDto event) {
        return new SecurityEvent(
                event.id(),
                event.recordedAt(),
                principalId(event),
                null,
                event.sourceIp(),
                action(event),
                resource(event.resources()),
                status(event.statusCode())
        );
    }

    public SecurityEventEntity toEntity(AuditEventDto event) {
        SecurityEventEntity entity = new SecurityEventEntity(
                event.id(),
                event.recordedAt(),
                event.locality(),
                event.organizationId(),
                event.sourceIp(),
                event.userAgent(),
                event.productName(),
                event.serviceName(),
                event.methodName(),
                event.requestId(),
                event.requestBody(),
                event.statusCode()
        );
        entity.setActorPrincipalId(principalId(event));
        entity.setResources(resourceMaps(event.resources()));
        applyIamUser(entity, event.resources());
        return entity;
    }

    private static void applyIamUser(SecurityEventEntity entity, List<ResourceDto> resources) {
        if (resources == null) {
            return;
        }
        for (ResourceDto resource : resources) {
            if (resource == null || !"iam_user".equals(resource.type())) {
                continue;
            }
            if (!isBlank(resource.id())) {
                entity.setScalewayUserId(resource.id());
            }
            if (!isBlank(resource.name())) {
                entity.setScalewayUsername(resource.name());
            }
            return;
        }
    }

    private static String principalId(AuditEventDto event) {
        if (event.principal() == null) {
            return null;
        }
        return event.principal().id();
    }

    private static String action(AuditEventDto event) {
        String service = event.serviceName();
        String method = event.methodName();
        if (isBlank(service)) {
            return method;
        }
        if (isBlank(method)) {
            return service;
        }
        return service + "." + method;
    }

    private static String resource(List<ResourceDto> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        String value = resources.stream()
                .map(ScalewayEventMapper::formatResource)
                .filter(item -> !isBlank(item))
                .collect(Collectors.joining(", "));
        return value.isBlank() ? null : value;
    }

    private static List<Map<String, Object>> resourceMaps(List<ResourceDto> resources) {
        if (resources == null || resources.isEmpty()) {
            return List.of();
        }
        return resources.stream()
                .filter(resource -> resource != null)
                .map(ScalewayEventMapper::resourceMap)
                .toList();
    }

    private static Map<String, Object> resourceMap(ResourceDto resource) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", resource.id());
        values.put("type", resource.type());
        values.put("name", resource.name());
        return values;
    }

    private static String formatResource(ResourceDto resource) {
        if (resource == null) {
            return null;
        }
        String identity = firstNonBlank(resource.name(), resource.id());
        if (isBlank(resource.type())) {
            return identity;
        }
        if (identity == null) {
            return resource.type();
        }
        return resource.type() + "/" + identity;
    }

    private static String status(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "success";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "forbidden";
        }
        return "failure";
    }

    private static String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }
        if (!isBlank(second)) {
            return second;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
