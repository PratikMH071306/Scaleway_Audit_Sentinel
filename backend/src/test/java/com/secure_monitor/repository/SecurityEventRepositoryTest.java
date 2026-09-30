package com.secure_monitor.repository;

import com.secure_monitor.domain.SecurityEventEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityEventRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private SecurityEventRepository repository;

    @Test
    void persistsJsonColumnsAndFindsEventsInTheLookbackWindow() {
        Instant recordedAt = Instant.parse("2026-09-30T00:00:00Z");
        SecurityEventEntity event = new SecurityEventEntity(
                "evt-1",
                recordedAt,
                "fr-par",
                "org-1",
                "203.0.113.10",
                "agent",
                "iam",
                "iam",
                "Login",
                "req-1",
                Map.of("username", "alice"),
                401
        );
        event.setResources(List.of(Map.of("id", "user-1", "type", "user")));
        repository.saveAndFlush(event);

        SecurityEventEntity stored = repository.findById("evt-1").orElseThrow();
        assertThat(stored.getRequestBody()).containsEntry("username", "alice");
        assertThat(stored.getResources()).containsExactly(Map.of("id", "user-1", "type", "user"));

        List<SecurityEventEntity> matches = repository
                .findByServiceNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                        "iam",
                        recordedAt.minusSeconds(1)
                );
        assertThat(matches).extracting(SecurityEventEntity::getEventId).containsExactly("evt-1");
    }
}
