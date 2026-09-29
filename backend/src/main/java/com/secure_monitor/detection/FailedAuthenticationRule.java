package com.secure_monitor.detection;

import com.secure_monitor.domain.SecurityEventEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FailedAuthenticationRule implements DetectionRule {

    public static final String RULE_NAME = "SUSPICIOUS_AUTHENTICATION";
    public static final String AUTHENTICATION_SERVICE = "scaleway.iam.v1alpha1.UnauthenticatedApi";
    static final String PASSWORD_LOGIN_METHOD = "CreatePasswordLogin";
    static final String COMMIT_LOGIN_METHOD = "CommitLogin";
    static final int FAILED_STATUS = 401;
    static final int SUCCESS_STATUS = 200;
    static final int THRESHOLD = 5;
    static final Duration WINDOW = Duration.ofMinutes(10);

    @Override
    public List<DetectionResult> evaluate(List<SecurityEventEntity> events) {
        Map<Subject, List<SecurityEventEntity>> failuresBySubject = new LinkedHashMap<>();
        for (SecurityEventEntity event : events) {
            if (!isFailedPasswordLogin(event)) {
                continue;
            }
            failuresBySubject
                    .computeIfAbsent(subjectOf(event), ignored -> new ArrayList<>())
                    .add(event);
        }

        List<DetectionResult> results = new ArrayList<>();
        for (Map.Entry<Subject, List<SecurityEventEntity>> entry : failuresBySubject.entrySet()) {
            SpikeWindow window = densestWindow(entry.getValue());
            SecurityEventEntity success = successAfter(events, entry.getKey(), window.startedAt());
            boolean detected = window.eventCount() >= THRESHOLD;
            boolean followedBySuccess = detected && success != null;
            List<String> relatedEventIds = relatedEventIds(window, success, followedBySuccess);
            String description = detected
                    ? description(window.eventCount(), entry.getKey(), followedBySuccess)
                    : null;
            results.add(new DetectionResult(
                    detected,
                    RULE_NAME,
                    description,
                    window.eventCount(),
                    entry.getKey().sourceIp(),
                    window.detectedAt(),
                    relatedEventIds,
                    entry.getKey().organizationId(),
                    entry.getKey().username(),
                    followedBySuccess
            ));
        }
        return results;
    }

    private static String description(int eventCount, Subject subject, boolean followedBySuccess) {
        String description = eventCount
                + " failed password login attempts for user "
                + subject.username()
                + " from "
                + subject.sourceIp()
                + " within "
                + WINDOW.toMinutes()
                + " minutes";
        if (followedBySuccess) {
            return description + ", followed by a successful login";
        }
        return description;
    }

    private static List<String> relatedEventIds(
            SpikeWindow window,
            SecurityEventEntity success,
            boolean followedBySuccess
    ) {
        if (!followedBySuccess) {
            return window.relatedEventIds();
        }
        List<String> ids = new ArrayList<>(window.relatedEventIds());
        ids.add(success.getEventId());
        return List.copyOf(ids);
    }

    private static boolean isFailedPasswordLogin(SecurityEventEntity event) {
        return isGroupedAuthenticationEvent(event)
                && PASSWORD_LOGIN_METHOD.equals(event.getMethodName())
                && event.getStatusCode() == FAILED_STATUS;
    }

    private static boolean isSuccessfulLogin(SecurityEventEntity event) {
        return isGroupedAuthenticationEvent(event)
                && COMMIT_LOGIN_METHOD.equals(event.getMethodName())
                && event.getStatusCode() == SUCCESS_STATUS;
    }

    private static boolean isGroupedAuthenticationEvent(SecurityEventEntity event) {
        if (event.getRecordedAt() == null || username(event) == null) {
            return false;
        }
        if (event.getSourceIp() == null || event.getSourceIp().isBlank()) {
            return false;
        }
        if (event.getOrganizationId() == null || event.getOrganizationId().isBlank()) {
            return false;
        }
        return AUTHENTICATION_SERVICE.equals(event.getServiceName());
    }

    private static Subject subjectOf(SecurityEventEntity event) {
        return new Subject(event.getOrganizationId(), username(event), event.getSourceIp());
    }

    static String username(SecurityEventEntity event) {
        String fromBody = text(event.getRequestBody() == null ? null : event.getRequestBody().get("username"));
        if (fromBody != null) {
            return fromBody;
        }
        List<Map<String, Object>> resources = event.getResources();
        if (resources == null) {
            return null;
        }
        String fallback = null;
        for (Map<String, Object> resource : resources) {
            if (resource == null) {
                continue;
            }
            String name = text(resource.get("name"));
            if (name == null) {
                continue;
            }
            if ("iam_user".equals(text(resource.get("type")))) {
                return name;
            }
            if (fallback == null) {
                fallback = name;
            }
        }
        return fallback;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static SecurityEventEntity successAfter(List<SecurityEventEntity> events, Subject subject, Instant windowStart) {
        Instant latest = windowStart.plus(WINDOW);
        SecurityEventEntity match = null;
        for (SecurityEventEntity event : events) {
            if (!isSuccessfulLogin(event) || !subject.equals(subjectOf(event))) {
                continue;
            }
            Instant recordedAt = event.getRecordedAt();
            if (!recordedAt.isAfter(windowStart) || recordedAt.isAfter(latest)) {
                continue;
            }
            if (match == null || recordedAt.isBefore(match.getRecordedAt())) {
                match = event;
            }
        }
        return match;
    }

    private static SpikeWindow densestWindow(List<SecurityEventEntity> events) {
        List<SecurityEventEntity> sorted = events.stream()
                .sorted(Comparator.comparing(SecurityEventEntity::getRecordedAt))
                .toList();
        int bestLeft = 0;
        int bestRight = 0;
        int left = 0;
        for (int right = 0; right < sorted.size(); right++) {
            Instant earliest = sorted.get(right).getRecordedAt().minus(WINDOW);
            while (sorted.get(left).getRecordedAt().isBefore(earliest)) {
                left++;
            }
            if (right - left > bestRight - bestLeft) {
                bestLeft = left;
                bestRight = right;
            }
        }
        List<SecurityEventEntity> window = sorted.subList(bestLeft, bestRight + 1);
        List<String> relatedEventIds = window.stream()
                .map(SecurityEventEntity::getEventId)
                .toList();
        return new SpikeWindow(
                window.size(),
                window.get(0).getRecordedAt(),
                window.get(window.size() - 1).getRecordedAt(),
                relatedEventIds
        );
    }

    private record Subject(String organizationId, String username, String sourceIp) {
    }

    private record SpikeWindow(
            int eventCount,
            Instant startedAt,
            Instant detectedAt,
            List<String> relatedEventIds
    ) {
    }
}
