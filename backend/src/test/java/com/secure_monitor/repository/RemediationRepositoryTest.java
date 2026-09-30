package com.secure_monitor.repository;

import com.secure_monitor.domain.RemediationEntity;
import com.secure_monitor.remediation.RemediationAction;
import com.secure_monitor.remediation.RemediationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RemediationRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private RemediationRepository repository;

    @Test
    void listsRemediationsForAnAlertNewestFirst() {
        UUID alertId = UUID.randomUUID();
        RemediationEntity older = new RemediationEntity(
                alertId,
                RemediationAction.LOCK_USER,
                "scw-user",
                "alice",
                UUID.randomUUID(),
                "pratik",
                false,
                Instant.parse("2026-09-30T01:00:00Z")
        );
        older.succeed(true, "locked", Instant.parse("2026-09-30T01:01:00Z"));
        RemediationEntity newer = new RemediationEntity(
                alertId,
                RemediationAction.UNLOCK_USER,
                "scw-user",
                "alice",
                UUID.randomUUID(),
                "pratik",
                true,
                Instant.parse("2026-09-30T02:00:00Z")
        );
        repository.saveAndFlush(older);
        repository.saveAndFlush(newer);

        assertThat(repository.findByAlertIdOrderByCreatedAtDesc(alertId))
                .extracting(RemediationEntity::getAction)
                .containsExactly(RemediationAction.UNLOCK_USER, RemediationAction.LOCK_USER);
        assertThat(repository.findByAlertIdOrderByCreatedAtDesc(alertId).get(1).getStatus())
                .isEqualTo(RemediationStatus.SUCCESS);
    }
}
