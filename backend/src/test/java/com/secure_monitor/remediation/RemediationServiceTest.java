package com.secure_monitor.remediation;

import com.secure_monitor.alert.AlertSeverity;
import com.secure_monitor.alert.AlertStatus;
import com.secure_monitor.domain.AlertEntity;
import com.secure_monitor.domain.RemediationEntity;
import com.secure_monitor.repository.AlertRepository;
import com.secure_monitor.repository.RemediationRepository;
import com.secure_monitor.scaleway.ScalewayService;
import com.secure_monitor.scaleway.dto.ScalewayUserDto;
import com.secure_monitor.scaleway.iam.ScalewayIamClient;
import com.secure_monitor.security.MonitorUserDetails;
import com.secure_monitor.domain.UserEntity;
import com.secure_monitor.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemediationServiceTest {

    @Mock
    private AlertRepository alerts;

    @Mock
    private RemediationRepository remediations;

    @Mock
    private ScalewayIamClient iamClient;

    @Mock
    private com.secure_monitor.scaleway.iam.ScalewayUserResolver userResolver;

    @Mock
    private ScalewayService scalewayService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void locksAnUnlockedUserAndVerifiesTheResult() {
        authenticate();
        UUID alertId = UUID.randomUUID();
        AlertEntity alert = alert(alertId, AlertStatus.OPEN, "user-1");
        when(alerts.findById(alertId)).thenReturn(Optional.of(alert));
        when(userResolver.attachUserId(alert)).thenReturn(alert);
        when(iamClient.getUser("user-1")).thenReturn(
                user(false),
                user(true)
        );
        when(iamClient.lockUser("user-1")).thenReturn(user(true));
        when(remediations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RemediationResponse response = service().remediate(alertId, new RemediationRequest(RemediationAction.LOCK_USER));

        assertThat(response.beforeLocked()).isFalse();
        assertThat(response.afterLocked()).isTrue();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("User locked successfully");
        ArgumentCaptor<RemediationEntity> saved = ArgumentCaptor.forClass(RemediationEntity.class);
        verify(remediations).save(saved.capture());
        assertThat(saved.getValue().getPerformedByUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getBeforeState()).isEqualTo("unlocked");
        assertThat(saved.getValue().getAfterState()).isEqualTo("locked");
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    void unlocksALockedUserAndVerifiesTheResult() {
        authenticate();
        UUID alertId = UUID.randomUUID();
        AlertEntity alert = alert(alertId, AlertStatus.OPEN, "user-1");
        when(alerts.findById(alertId)).thenReturn(Optional.of(alert));
        when(userResolver.attachUserId(alert)).thenReturn(alert);
        when(iamClient.getUser("user-1")).thenReturn(user(true), user(false));
        when(iamClient.unlockUser("user-1")).thenReturn(user(false));
        when(remediations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RemediationResponse response = service().remediate(alertId, new RemediationRequest(RemediationAction.UNLOCK_USER));

        assertThat(response.beforeLocked()).isTrue();
        assertThat(response.afterLocked()).isFalse();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("User unlocked successfully");
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    void doesNotCallLockWhenTheUserIsAlreadyLocked() {
        authenticate();
        UUID alertId = UUID.randomUUID();
        AlertEntity alert = alert(alertId, AlertStatus.OPEN, "user-1");
        when(alerts.findById(alertId)).thenReturn(Optional.of(alert));
        when(userResolver.attachUserId(alert)).thenReturn(alert);
        when(iamClient.getUser("user-1")).thenReturn(user(true));
        when(remediations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RemediationResponse response = service().remediate(alertId, new RemediationRequest(RemediationAction.LOCK_USER));

        verify(iamClient, never()).lockUser(any());
        assertThat(response.beforeLocked()).isTrue();
        assertThat(response.afterLocked()).isTrue();
        assertThat(response.message()).isEqualTo("User is already locked");
    }

    @Test
    void rejectsAnAlertWithoutAScalewayUserId() {
        UUID alertId = UUID.randomUUID();
        AlertEntity alert = alert(alertId, AlertStatus.OPEN, null);
        when(alerts.findById(alertId)).thenReturn(Optional.of(alert));
        when(userResolver.attachUserId(alert)).thenReturn(alert);

        assertThatThrownBy(() -> service().remediate(alertId, new RemediationRequest(RemediationAction.LOCK_USER)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        verify(iamClient, never()).getUser(any());
    }

    private RemediationService service() {
        return new RemediationService(alerts, remediations, iamClient, userResolver, scalewayService);
    }

    private static void authenticate() {
        UserEntity user = new UserEntity("admin", "hash", Role.ADMIN, true);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        MonitorUserDetails details = new MonitorUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );
    }

    private static AlertEntity alert(UUID id, AlertStatus status, String scalewayUserId) {
        AlertEntity entity = new AlertEntity(
                "SUSPICIOUS_AUTHENTICATION",
                AlertSeverity.HIGH,
                status,
                "Suspicious authentication activity",
                "failures",
                Instant.parse("2026-09-29T08:00:00Z"),
                "122.167.0.1",
                "org",
                "pratikraorane65",
                scalewayUserId,
                5,
                List.of("e1")
        );
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static ScalewayUserDto user(boolean locked) {
        return new ScalewayUserDto("user-1", "pratikraorane65", null, "org", locked, "user", null);
    }
}
