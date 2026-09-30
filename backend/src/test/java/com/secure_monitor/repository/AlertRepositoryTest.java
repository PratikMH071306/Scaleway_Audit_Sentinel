package com.secure_monitor.repository;

import com.secure_monitor.alert.AlertSeverity;
import com.secure_monitor.alert.AlertStatus;
import com.secure_monitor.domain.AlertEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private AlertRepository repository;

    @Test
    void storesRelatedEventIdsAsJsonAndMatchesAnOpenAlert() {
        AlertEntity alert = new AlertEntity(
                "failed-auth",
                AlertSeverity.HIGH,
                AlertStatus.OPEN,
                "Repeated login failures",
                "Several failed sign-ins from one address",
                Instant.parse("2026-09-30T01:00:00Z"),
                "203.0.113.10",
                "org-1",
                "alice",
                "scw-user",
                3,
                List.of("evt-1", "evt-2")
        );
        repository.saveAndFlush(alert);

        assertThat(repository.existsByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                "failed-auth", "org-1", "alice", "203.0.113.10", AlertStatus.OPEN
        )).isTrue();

        AlertEntity stored = repository.findFirstByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                "failed-auth", "org-1", "alice", "203.0.113.10", AlertStatus.OPEN
        ).orElseThrow();
        assertThat(stored.getRelatedEventIds()).containsExactly("evt-1", "evt-2");
    }
}
