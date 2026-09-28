package com.secure_monitor.detection;

import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionServiceTest {

    @Mock
    private SecurityEventRepository repository;

    @Test
    void evaluatesRecentAuthenticationEvents() {
        Instant recordedAt = Instant.parse("2026-09-28T04:31:00Z");
        List<SecurityEventEntity> events = List.of(failedAuthentication(recordedAt));
        when(repository.findByServiceNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                eq(FailedAuthenticationRule.AUTHENTICATION_SERVICE),
                any()
        )).thenReturn(events);

        DetectionService service = new DetectionService(repository, new FailedAuthenticationRule());
        List<DetectionResult> results = service.detectFailedAuthentication();

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findByServiceNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                eq(FailedAuthenticationRule.AUTHENTICATION_SERVICE),
                since.capture()
        );
        assertThat(since.getValue()).isCloseTo(
                Instant.now().minus(DetectionService.RECENT_EVENT_LOOKBACK),
                within(5, ChronoUnit.SECONDS)
        );
        assertThat(results).containsExactly(new DetectionResult(
                false,
                "FAILED_AUTHENTICATION_SPIKE",
                null,
                1,
                "122.167.118.87"
        ));
    }

    private static SecurityEventEntity failedAuthentication(Instant recordedAt) {
        return new SecurityEventEntity(
                "auth-1",
                recordedAt,
                "fr-par",
                "org-1",
                "122.167.118.87",
                "console",
                FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                "Authenticate",
                "req-1",
                Map.of("result", "failed", "reason", "invalid_password"),
                401
        );
    }
}
