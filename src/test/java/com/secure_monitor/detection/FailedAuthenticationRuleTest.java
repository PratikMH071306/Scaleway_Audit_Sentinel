package com.secure_monitor.detection;

import com.secure_monitor.domain.SecurityEventEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FailedAuthenticationRuleTest {

    private final FailedAuthenticationRule rule = new FailedAuthenticationRule();

    @Test
    void detectsFiveFailedAuthenticationsFromTheSameSourceWithinFiveMinutes() {
        Instant start = Instant.parse("2026-09-28T04:31:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        events.add(renewJwt("jwt-1", start.minusSeconds(30), "122.167.118.87"));
        for (int minute = 0; minute < 5; minute++) {
            events.add(failedAuthentication(
                    "auth-" + minute,
                    start.plusSeconds(minute * 60L),
                    "122.167.118.87"
            ));
        }
        events.add(otherFailure("create-1", start, "122.167.118.87", 400));

        List<DetectionResult> results = rule.evaluate(events);

        assertThat(results).containsExactly(new DetectionResult(
                true,
                "FAILED_AUTHENTICATION_SPIKE",
                "5 failed authentication attempts detected within 5 minutes",
                5,
                "122.167.118.87"
        ));
    }

    @Test
    void detectsEverySourceThatReachesTheThreshold() {
        Instant start = Instant.parse("2026-09-28T04:31:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            events.add(failedAuthentication("a-" + minute, start.plusSeconds(minute * 60L), "122.167.118.87"));
            events.add(failedAuthentication("b-" + minute, start.plusSeconds(minute * 60L), "122.167.118.89"));
        }

        List<DetectionResult> results = rule.evaluate(events);

        assertThat(results).containsExactly(
                new DetectionResult(
                        true,
                        "FAILED_AUTHENTICATION_SPIKE",
                        "5 failed authentication attempts detected within 5 minutes",
                        5,
                        "122.167.118.87"
                ),
                new DetectionResult(
                        true,
                        "FAILED_AUTHENTICATION_SPIKE",
                        "5 failed authentication attempts detected within 5 minutes",
                        5,
                        "122.167.118.89"
                )
        );
    }

    @Test
    void reportsTheDenseWindowWhenTheThresholdIsNotMet() {
        Instant start = Instant.parse("2026-09-28T04:31:00Z");

        List<DetectionResult> results = rule.evaluate(List.of(
                failedAuthentication("auth-1", start, "122.167.118.87"),
                failedAuthentication("auth-2", start.plusSeconds(60), "122.167.118.87"),
                renewJwt("jwt-1", start, "122.167.118.87")
        ));

        assertThat(results).containsExactly(new DetectionResult(
                false,
                "FAILED_AUTHENTICATION_SPIKE",
                null,
                2,
                "122.167.118.87"
        ));
    }

    @Test
    void doesNotCombineFailuresOutsideTheWindowOrFromAnotherSource() {
        Instant start = Instant.parse("2026-09-28T04:31:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        for (int minute = 0; minute < 4; minute++) {
            events.add(failedAuthentication("a-" + minute, start.plusSeconds(minute * 60L), "122.167.118.87"));
        }
        events.add(failedAuthentication("late", start.plusSeconds(6 * 60L), "122.167.118.87"));
        for (int minute = 0; minute < 4; minute++) {
            events.add(failedAuthentication("b-" + minute, start.plusSeconds(minute * 60L), "203.0.113.10"));
        }

        List<DetectionResult> results = rule.evaluate(events);

        assertThat(results).extracting(DetectionResult::sourceIp).containsExactly("122.167.118.87", "203.0.113.10");
        assertThat(results).allSatisfy(result -> {
            assertThat(result.detected()).isFalse();
            assertThat(result.eventCount()).isEqualTo(4);
        });
    }

    @Test
    void ignoresAuthenticateCallsThatDidNotFail() {
        Instant start = Instant.parse("2026-09-28T04:31:00Z");

        List<DetectionResult> results = rule.evaluate(List.of(
                authentication("ok-1", start, "122.167.118.87", 200, Map.of("result", "success")),
                authentication("blank", start.plusSeconds(60), "122.167.118.87", 401, null)
        ));

        assertThat(results).isEmpty();
    }

    private static SecurityEventEntity failedAuthentication(String id, Instant recordedAt, String sourceIp) {
        return authentication(
                id,
                recordedAt,
                sourceIp,
                401,
                Map.of("result", "failed", "reason", "invalid_password")
        );
    }

    private static SecurityEventEntity authentication(
            String id,
            Instant recordedAt,
            String sourceIp,
            int statusCode,
            Map<String, Object> requestBody
    ) {
        return new SecurityEventEntity(
                id,
                recordedAt,
                "fr-par",
                "org-1",
                sourceIp,
                "console",
                FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                "Authenticate",
                "req-" + id,
                requestBody,
                statusCode
        );
    }

    private static SecurityEventEntity renewJwt(String id, Instant recordedAt, String sourceIp) {
        return new SecurityEventEntity(
                id,
                recordedAt,
                "fr-par",
                "org-1",
                sourceIp,
                "console",
                FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                "RenewJWT",
                "req-" + id,
                null,
                200
        );
    }

    private static SecurityEventEntity otherFailure(String id, Instant recordedAt, String sourceIp, int statusCode) {
        return new SecurityEventEntity(
                id,
                recordedAt,
                "fr-par",
                "org-1",
                sourceIp,
                "console",
                "scaleway.iam.v1alpha1.Api",
                "scaleway.iam.v1alpha1.Api",
                "CreateUser",
                "req-" + id,
                null,
                statusCode
        );
    }
}
