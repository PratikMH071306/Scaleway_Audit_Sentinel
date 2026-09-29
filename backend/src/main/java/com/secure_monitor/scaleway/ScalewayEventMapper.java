package com.secure_monitor.scaleway;

import com.secure_monitor.domain.SecurityEvent;
import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.scaleway.dto.AuditEventDto;
import com.secure_monitor.scaleway.dto.ResourceDto;
import org.springframework.stereotype.Component;

import java.util.List;
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
        return new SecurityEventEntity(
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
