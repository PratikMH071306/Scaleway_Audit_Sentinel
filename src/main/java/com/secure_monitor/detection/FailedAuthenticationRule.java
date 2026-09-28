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

    public static final String RULE_NAME = "FAILED_AUTHENTICATION_SPIKE";
    public static final String AUTHENTICATION_SERVICE = "scaleway.iam.v1alpha1.UnauthenticatedApi";
    static final String AUTHENTICATION_METHOD = "Authenticate";
    static final String FAILED_RESULT = "failed";
    static final int THRESHOLD = 5;
    static final Duration WINDOW = Duration.ofMinutes(5);

    @Override
    public List<DetectionResult> evaluate(List<SecurityEventEntity> events) {
        Map<String, List<SecurityEventEntity>> failuresBySource = new LinkedHashMap<>();
        for (SecurityEventEntity event : events) {
            if (!isFailedAuthentication(event)) {
                continue;
            }
            failuresBySource
                    .computeIfAbsent(event.getSourceIp(), ignored -> new ArrayList<>())
                    .add(event);
        }

        List<DetectionResult> results = new ArrayList<>();
        for (Map.Entry<String, List<SecurityEventEntity>> entry : failuresBySource.entrySet()) {
            SpikeWindow window = densestWindow(entry.getValue());
            boolean detected = window.eventCount() >= THRESHOLD;
            String description = detected
                    ? window.eventCount() + " failed authentication attempts detected within " + WINDOW.toMinutes() + " minutes"
                    : null;
            results.add(new DetectionResult(
                    detected,
                    RULE_NAME,
                    description,
                    window.eventCount(),
                    entry.getKey(),
                    window.detectedAt(),
                    window.relatedEventIds()
            ));
        }
        return results;
    }

    private static boolean isFailedAuthentication(SecurityEventEntity event) {
        if (event.getSourceIp() == null || event.getSourceIp().isBlank() || event.getRecordedAt() == null) {
            return false;
        }
        if (!AUTHENTICATION_SERVICE.equals(event.getServiceName())) {
            return false;
        }
        if (!AUTHENTICATION_METHOD.equals(event.getMethodName())) {
            return false;
        }
        Map<String, Object> body = event.getRequestBody();
        if (body == null) {
            return false;
        }
        return FAILED_RESULT.equals(String.valueOf(body.get("result")));
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
        return new SpikeWindow(window.size(), window.get(window.size() - 1).getRecordedAt(), relatedEventIds);
    }

    private record SpikeWindow(int eventCount, Instant detectedAt, List<String> relatedEventIds) {
    }
}
