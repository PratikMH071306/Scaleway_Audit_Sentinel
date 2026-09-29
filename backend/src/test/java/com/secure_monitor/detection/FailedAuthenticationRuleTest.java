package com.secure_monitor.detection;

import com.secure_monitor.domain.SecurityEventEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FailedAuthenticationRuleTest {

    private static final String ORG = "e37c8fea-0284-4f90-bbfe-04666c7ecfd6";
    private static final String USER = "test01";
    private static final String IP = "103.147.161.45";

    private final FailedAuthenticationRule rule = new FailedAuthenticationRule();

    @Test
    void detectsFiveFailedPasswordLoginsForTheSameUserAndSourceWithinTenMinutes() {
        Instant start = Instant.parse("2026-09-29T08:33:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        events.add(commitLogin("ok", start.minusSeconds(30), ORG, USER, IP));
        for (int minute = 0; minute < 5; minute++) {
            events.add(failedPasswordLogin("fail-" + minute, start.plusSeconds(minute * 60L), ORG, USER, IP));
        }
        events.add(otherFailure("create-1", start, IP));

        List<DetectionResult> results = rule.evaluate(events);

        assertThat(results).containsExactly(detected(
                5,
                start.plusSeconds(4 * 60L),
                List.of("fail-0", "fail-1", "fail-2", "fail-3", "fail-4"),
                false
        ));
    }

    @Test
    void raisesAStrongerSignalWhenTheSameUserLogsInSuccessfullyAfterward() {
        Instant start = Instant.parse("2026-09-29T08:33:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            events.add(failedPasswordLogin("fail-" + minute, start.plusSeconds(minute * 60L), ORG, USER, IP));
        }
        events.add(commitLogin("ok", start.plusSeconds(6 * 60L), ORG, USER, IP));

        List<DetectionResult> results = rule.evaluate(events);

        assertThat(results).containsExactly(detected(
                5,
                start.plusSeconds(4 * 60L),
                List.of("fail-0", "fail-1", "fail-2", "fail-3", "fail-4", "ok"),
                true
        ));
    }

    @Test
    void keepsUsersOnTheSameSourceSeparate() {
        Instant start = Instant.parse("2026-09-29T08:33:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            events.add(failedPasswordLogin("a-" + minute, start.plusSeconds(minute * 60L), ORG, "test01", IP));
            events.add(failedPasswordLogin("b-" + minute, start.plusSeconds(minute * 60L), ORG, "test02", IP));
        }

        List<DetectionResult> results = rule.evaluate(events);

        assertThat(results).extracting(DetectionResult::username).containsExactly("test01", "test02");
        assertThat(results).allSatisfy(result -> {
            assertThat(result.detected()).isTrue();
            assertThat(result.sourceIp()).isEqualTo(IP);
            assertThat(result.eventCount()).isEqualTo(5);
        });
    }

    @Test
    void doesNotCombineFailuresOutsideTheWindowOrFromAnotherSource() {
        Instant start = Instant.parse("2026-09-29T08:33:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        for (int minute = 0; minute < 4; minute++) {
            events.add(failedPasswordLogin("a-" + minute, start.plusSeconds(minute * 60L), ORG, USER, IP));
        }
        events.add(failedPasswordLogin("late", start.plusSeconds(11 * 60L), ORG, USER, IP));
        for (int minute = 0; minute < 4; minute++) {
            events.add(failedPasswordLogin("b-" + minute, start.plusSeconds(minute * 60L), ORG, USER, "203.0.113.10"));
        }

        List<DetectionResult> results = rule.evaluate(events);

        assertThat(results).extracting(DetectionResult::sourceIp).containsExactly(IP, "203.0.113.10");
        assertThat(results).allSatisfy(result -> {
            assertThat(result.detected()).isFalse();
            assertThat(result.eventCount()).isEqualTo(4);
            assertThat(result.followedBySuccess()).isFalse();
        });
    }

    @Test
    void ignoresSuccessfulPasswordLoginsAndCommitLoginByItself() {
        Instant start = Instant.parse("2026-09-29T08:42:17.944155805Z");

        List<DetectionResult> results = rule.evaluate(List.of(
                passwordLogin("ok-password", start, ORG, USER, IP, 200),
                commitLogin("ok-commit", start.plusSeconds(60), ORG, USER, IP)
        ));

        assertThat(results).isEmpty();
    }

    @Test
    void doesNotTreatASuccessfulLoginForAnotherUserAsTheStrongerSignal() {
        Instant start = Instant.parse("2026-09-29T08:33:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            events.add(failedPasswordLogin("fail-" + minute, start.plusSeconds(minute * 60L), ORG, USER, IP));
        }
        events.add(commitLogin("other-user", start.plusSeconds(6 * 60L), ORG, "test02", IP));

        DetectionResult result = rule.evaluate(events).get(0);

        assertThat(result.followedBySuccess()).isFalse();
        assertThat(result.relatedEventIds()).doesNotContain("other-user");
    }

    private static DetectionResult detected(
            int eventCount,
            Instant detectedAt,
            List<String> relatedEventIds,
            boolean followedBySuccess
    ) {
        String description = eventCount
                + " failed password login attempts for user "
                + USER
                + " from "
                + IP
                + " within 10 minutes";
        if (followedBySuccess) {
            description += ", followed by a successful login";
        }
        return new DetectionResult(
                true,
                "SUSPICIOUS_AUTHENTICATION",
                description,
                eventCount,
                IP,
                detectedAt,
                relatedEventIds,
                ORG,
                USER,
                followedBySuccess
        );
    }

    private static SecurityEventEntity failedPasswordLogin(
            String id,
            Instant recordedAt,
            String organizationId,
            String username,
            String sourceIp
    ) {
        return passwordLogin(id, recordedAt, organizationId, username, sourceIp, 401);
    }

    private static SecurityEventEntity passwordLogin(
            String id,
            Instant recordedAt,
            String organizationId,
            String username,
            String sourceIp,
            int statusCode
    ) {
        return event(
                id,
                recordedAt,
                organizationId,
                sourceIp,
                FailedAuthenticationRule.PASSWORD_LOGIN_METHOD,
                Map.of("organization_id", organizationId, "password", "REDACTED", "username", username),
                statusCode,
                null
        );
    }

    private static SecurityEventEntity commitLogin(
            String id,
            Instant recordedAt,
            String organizationId,
            String username,
            String sourceIp
    ) {
        SecurityEventEntity event = event(
                id,
                recordedAt,
                organizationId,
                sourceIp,
                FailedAuthenticationRule.COMMIT_LOGIN_METHOD,
                Map.of("login_id", "e679c325-3973-4075-bfa3-b29ba815833a"),
                200,
                null
        );
        event.setResources(List.of(Map.of(
                "id", "ead5aed3-1d91-4201-b388-98c2febfc404",
                "type", "iam_user",
                "name", username
        )));
        return event;
    }

    private static SecurityEventEntity otherFailure(String id, Instant recordedAt, String sourceIp) {
        return event(
                id,
                recordedAt,
                ORG,
                sourceIp,
                "CreateUser",
                null,
                400,
                "scaleway.iam.v1alpha1.Api"
        );
    }

    private static SecurityEventEntity event(
            String id,
            Instant recordedAt,
            String organizationId,
            String sourceIp,
            String methodName,
            Map<String, Object> requestBody,
            int statusCode,
            String serviceName
    ) {
        return new SecurityEventEntity(
                id,
                recordedAt,
                "global",
                organizationId,
                sourceIp,
                "Mozilla/5.0",
                "iam",
                serviceName == null ? FailedAuthenticationRule.AUTHENTICATION_SERVICE : serviceName,
                methodName,
                "req-" + id,
                requestBody,
                statusCode
        );
    }
}
