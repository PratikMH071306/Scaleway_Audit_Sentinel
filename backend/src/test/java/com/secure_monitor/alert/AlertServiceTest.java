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

    private static final String SOURCE_A = "103.147.161.45";
    private static final String SOURCE_B = "203.0.113.10";
    private static final String ORG = "e37c8fea-0284-4f90-bbfe-04666c7ecfd6";
    private static final String USER = "test01";
    private static final Instant DETECTED_AT = Instant.parse("2026-09-28T10:30:00Z");
    private static final String DESCRIPTION = "5 failed password login attempts for user test01 from 103.147.161.45 within 10 minutes";

    @Mock
    private AlertRepository repository;

    @Test
    void createsOneOpenAlertFromADetection() {
        when(repository.existsByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                FailedAuthenticationRule.RULE_NAME,
                ORG,
                USER,
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
        assertThat(entity.getTitle()).isEqualTo(AlertService.SUSPICIOUS_AUTHENTICATION_TITLE);
        assertThat(entity.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(entity.getDetectedAt()).isEqualTo(DETECTED_AT);
        assertThat(entity.getSourceIp()).isEqualTo(SOURCE_A);
        assertThat(entity.getOrganizationId()).isEqualTo(ORG);
        assertThat(entity.getUsername()).isEqualTo(USER);
        assertThat(entity.getEventCount()).isEqualTo(5);
        assertThat(entity.getRelatedEventIds()).containsExactly("e1", "e2", "e3", "e4", "e5");
        assertThat(response.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(response.sourceIp()).isEqualTo(SOURCE_A);
    }

    @Test
    void doesNotCreateADuplicateWhileTheSameSourceStaysOpen() {
        UUID existingId = UUID.fromString("8e5c0000-0000-4000-8000-000000000001");
        when(repository.existsByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                FailedAuthenticationRule.RULE_NAME,
                ORG,
                USER,
                SOURCE_A,
                AlertStatus.OPEN
        )).thenReturn(false, true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findFirstByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                FailedAuthenticationRule.RULE_NAME,
                ORG,
                USER,
                SOURCE_A,
                AlertStatus.OPEN
        )).thenReturn(Optional.of(openAlert(existingId, SOURCE_A, USER)));

        AlertService service = new AlertService(repository);
        service.record(spike(SOURCE_A));
        AlertResponse second = service.record(spike(SOURCE_A));

        verify(repository, times(1)).save(any());
        assertThat(second.id()).isEqualTo(existingId);
        assertThat(second.status()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    void createsASeparateAlertForEachSource() {
        when(repository.existsByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                any(), any(), any(), any(), any()
        )).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AlertService service = new AlertService(repository);
        service.record(spike(SOURCE_A, USER));
        service.record(spike(SOURCE_B, USER));
        service.record(spike(SOURCE_A, "test02"));

        ArgumentCaptor<AlertEntity> saved = ArgumentCaptor.forClass(AlertEntity.class);
        verify(repository, times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(AlertEntity::getSourceIp).containsExactly(SOURCE_A, SOURCE_B, SOURCE_A);
        assertThat(saved.getAllValues()).extracting(AlertEntity::getUsername).containsExactly(USER, USER, "test02");
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
                List.of("e1", "e2"),
                ORG,
                USER,
                false
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
        return spike(sourceIp, USER);
    }

    private static DetectionResult spike(String sourceIp, String username) {
        return new DetectionResult(
                true,
                FailedAuthenticationRule.RULE_NAME,
                DESCRIPTION,
                5,
                sourceIp,
                DETECTED_AT,
                List.of("e1", "e2", "e3", "e4", "e5"),
                ORG,
                username,
                false
        );
    }

    @Test
    void marksAnOpenAlertResolved() {
        UUID id = UUID.fromString("8e5c0000-0000-4000-8000-000000000001");
        AlertEntity entity = openAlert(id, SOURCE_A, USER);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        AlertResponse response = new AlertService(repository).resolve(id);

        assertThat(response.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(entity.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    }

    private static AlertEntity openAlert(UUID id, String sourceIp, String username) {
        AlertEntity entity = new AlertEntity(
                FailedAuthenticationRule.RULE_NAME,
                AlertSeverity.HIGH,
                AlertStatus.OPEN,
                AlertService.SUSPICIOUS_AUTHENTICATION_TITLE,
                DESCRIPTION,
                DETECTED_AT,
                sourceIp,
                ORG,
                username,
                5,
                List.of("e1", "e2", "e3", "e4", "e5")
        );
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
