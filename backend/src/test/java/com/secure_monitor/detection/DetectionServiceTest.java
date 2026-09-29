package com.secure_monitor.detection;

import com.secure_monitor.alert.AlertService;
import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionServiceTest {

    @Mock
    private SecurityEventRepository repository;

    @Mock
    private AlertService alertService;

    @Test
    void evaluatesRecentAuthenticationEvents() {
        Instant recordedAt = Instant.parse("2026-09-28T04:31:00Z");
        List<SecurityEventEntity> events = List.of(failedAuthentication(recordedAt));
        when(repository.findByServiceNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                eq(FailedAuthenticationRule.AUTHENTICATION_SERVICE),
                any()
        )).thenReturn(events);

        DetectionService service = new DetectionService(repository, new FailedAuthenticationRule(), alertService);
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
                "SUSPICIOUS_AUTHENTICATION",
                null,
                1,
                "103.147.161.45",
                recordedAt,
                List.of("auth-1"),
                "org-1",
                "test01",
                false,
                null
        ));
        verify(alertService, never()).record(any());
    }

    @Test
    void recordsAnAlertWhenTheRuleDetectsASpike() {
        Instant start = Instant.parse("2026-09-28T04:31:00Z");
        List<SecurityEventEntity> events = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            events.add(failedAuthentication("auth-" + minute, start.plusSeconds(minute * 60L)));
        }
        when(repository.findByServiceNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                eq(FailedAuthenticationRule.AUTHENTICATION_SERVICE),
                any()
        )).thenReturn(events);

        DetectionService service = new DetectionService(repository, new FailedAuthenticationRule(), alertService);
        service.detectFailedAuthentication();

        verify(alertService).record(new DetectionResult(
                true,
                "SUSPICIOUS_AUTHENTICATION",
                "5 failed password login attempts for user test01 from 103.147.161.45 within 10 minutes",
                5,
                "103.147.161.45",
                start.plusSeconds(4 * 60L),
                List.of("auth-0", "auth-1", "auth-2", "auth-3", "auth-4"),
                "org-1",
                "test01",
                false,
                null
        ));
    }

    private static SecurityEventEntity failedAuthentication(Instant recordedAt) {
        return failedAuthentication("auth-1", recordedAt);
    }

    private static SecurityEventEntity failedAuthentication(String id, Instant recordedAt) {
        return new SecurityEventEntity(
                id,
                recordedAt,
                "fr-par",
                "org-1",
                "103.147.161.45",
                "Mozilla/5.0",
                "iam",
                FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                "CreatePasswordLogin",
                "req-1",
                Map.of("username", "test01", "password", "REDACTED"),
                401
        );
    }
}
