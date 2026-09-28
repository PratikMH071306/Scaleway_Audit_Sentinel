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
            int eventCount = countWithinWindow(entry.getValue());
            boolean detected = eventCount >= THRESHOLD;
            String description = detected
                    ? eventCount + " failed authentication attempts detected within " + WINDOW.toMinutes() + " minutes"
                    : null;
            results.add(new DetectionResult(detected, RULE_NAME, description, eventCount, entry.getKey()));
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

    private static int countWithinWindow(List<SecurityEventEntity> events) {
        List<Instant> times = events.stream()
                .map(SecurityEventEntity::getRecordedAt)
                .sorted(Comparator.naturalOrder())
                .toList();
        int best = 0;
        int left = 0;
        for (int right = 0; right < times.size(); right++) {
            Instant earliest = times.get(right).minus(WINDOW);
            while (times.get(left).isBefore(earliest)) {
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }
}
