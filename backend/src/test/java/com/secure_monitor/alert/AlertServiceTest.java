package com.secure_monitor.alert;

import com.secure_monitor.detection.DetectionResult;
import com.secure_monitor.detection.FailedAuthenticationRule;
import com.secure_monitor.domain.AlertEntity;
import com.secure_monitor.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final String SOURCE_A = "122.167.118.87";
    private static final String SOURCE_B = "203.0.113.10";
    private static final Instant DETECTED_AT = Instant.parse("2026-09-28T10:30:00Z");

    @Mock
    private AlertRepository repository;

    @Test
    void createsOneOpenAlertFromADetection() {
        when(repository.existsByRuleAndSourceIpAndStatus(
                FailedAuthenticationRule.RULE_NAME,
                SOURCE_A,
                AlertStatus.OPEN
        )).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AlertService service = new AlertService(repository);
        AlertResponse response = service.record(spike(SOURCE_A));

        ArgumentCaptor<AlertEntity> saved = ArgumentCaptor.forClass(AlertEntity.class);
        verify(repository).save(saved.capture());
        AlertEntity entity = saved.getValue();
        assertThat(entity.getRule()).isEqualTo(FailedAuthenticationRule.RULE_NAME);
        assertThat(entity.getSeverity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(entity.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(entity.getTitle()).isEqualTo(AlertService.FAILED_AUTHENTICATION_TITLE);
        assertThat(entity.getDescription()).isEqualTo("5 failed authentication attempts detected within 5 minutes");
        assertThat(entity.getDetectedAt()).isEqualTo(DETECTED_AT);
        assertThat(entity.getSourceIp()).isEqualTo(SOURCE_A);
        assertThat(entity.getEventCount()).isEqualTo(5);
        assertThat(entity.getRelatedEventIds()).containsExactly("e1", "e2", "e3", "e4", "e5");
        assertThat(response.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(response.sourceIp()).isEqualTo(SOURCE_A);
    }

    @Test
    void doesNotCreateADuplicateWhileTheSameSourceStaysOpen() {
        UUID existingId = UUID.fromString("8e5c0000-0000-4000-8000-000000000001");
        when(repository.existsByRuleAndSourceIpAndStatus(
                FailedAuthenticationRule.RULE_NAME,
                SOURCE_A,
                AlertStatus.OPEN
        )).thenReturn(false, true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findFirstByRuleAndSourceIpAndStatus(
                FailedAuthenticationRule.RULE_NAME,
                SOURCE_A,
                AlertStatus.OPEN
        )).thenReturn(Optional.of(openAlert(existingId, SOURCE_A)));

        AlertService service = new AlertService(repository);
        service.record(spike(SOURCE_A));
        AlertResponse second = service.record(spike(SOURCE_A));

        verify(repository, times(1)).save(any());
        assertThat(second.id()).isEqualTo(existingId);
        assertThat(second.status()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    void createsASeparateAlertForEachSource() {
        when(repository.existsByRuleAndSourceIpAndStatus(any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AlertService service = new AlertService(repository);
        service.record(spike(SOURCE_A));
        service.record(spike(SOURCE_B));

        ArgumentCaptor<AlertEntity> saved = ArgumentCaptor.forClass(AlertEntity.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(AlertEntity::getSourceIp).containsExactly(SOURCE_A, SOURCE_B);
        assertThat(saved.getAllValues()).allSatisfy(entity -> assertThat(entity.getStatus()).isEqualTo(AlertStatus.OPEN));
    }

    @Test
    void doesNotRecordActivityThatWasNotDetected() {
        AlertService service = new AlertService(repository);

        assertThatThrownBy(() -> service.record(new DetectionResult(
                false,
                FailedAuthenticationRule.RULE_NAME,
                null,
                2,
                SOURCE_A,
                DETECTED_AT,
                List.of("e1", "e2")
        ))).isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void returnsNotFoundWhenTheAlertDoesNotExist() {
        UUID id = UUID.fromString("8e5c0000-0000-4000-8000-000000000099");
        when(repository.findById(id)).thenReturn(Optional.empty());

        AlertService service = new AlertService(repository);

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static DetectionResult spike(String sourceIp) {
        return new DetectionResult(
                true,
                FailedAuthenticationRule.RULE_NAME,
                "5 failed authentication attempts detected within 5 minutes",
                5,
                sourceIp,
                DETECTED_AT,
                List.of("e1", "e2", "e3", "e4", "e5")
        );
    }

    private static AlertEntity openAlert(UUID id, String sourceIp) {
        AlertEntity entity = new AlertEntity(
                FailedAuthenticationRule.RULE_NAME,
                AlertSeverity.HIGH,
                AlertStatus.OPEN,
                AlertService.FAILED_AUTHENTICATION_TITLE,
                "5 failed authentication attempts detected within 5 minutes",
                DETECTED_AT,
                sourceIp,
                5,
                List.of("e1", "e2", "e3", "e4", "e5")
        );
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
