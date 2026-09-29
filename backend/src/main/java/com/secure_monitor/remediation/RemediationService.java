package com.secure_monitor.remediation;

import com.secure_monitor.alert.AlertStatus;
import com.secure_monitor.domain.AlertEntity;
import com.secure_monitor.domain.RemediationEntity;
import com.secure_monitor.repository.AlertRepository;
import com.secure_monitor.repository.RemediationRepository;
import com.secure_monitor.scaleway.ScalewayService;
import com.secure_monitor.scaleway.dto.ScalewayUserDto;
import com.secure_monitor.scaleway.iam.ScalewayApiException;
import com.secure_monitor.scaleway.iam.ScalewayIamClient;
import com.secure_monitor.scaleway.iam.ScalewayUserResolver;
import com.secure_monitor.security.AuthenticationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RemediationService {

    private final AlertRepository alerts;
    private final RemediationRepository remediations;
    private final ScalewayIamClient iamClient;
    private final ScalewayUserResolver userResolver;
    private final ScalewayService scalewayService;

    public RemediationService(
            AlertRepository alerts,
            RemediationRepository remediations,
            ScalewayIamClient iamClient,
            ScalewayUserResolver userResolver,
            ScalewayService scalewayService
    ) {
        this.alerts = alerts;
        this.remediations = remediations;
        this.iamClient = iamClient;
        this.userResolver = userResolver;
        this.scalewayService = scalewayService;
    }

    public RemediationResponse remediate(UUID alertId, RemediationRequest request) {
        if (request == null || request.action() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A remediation action is required");
        }
        if (request.action() != RemediationAction.LOCK_USER && request.action() != RemediationAction.UNLOCK_USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported remediation action");
        }

        AlertEntity alert = alerts.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an open alert can be remediated");
        }
        userResolver.attachUserId(alert);
        if (alert.getScalewayUserId() == null || alert.getScalewayUserId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "This alert has no Scaleway IAM user id"
            );
        }

        ScalewayUserDto before;
        try {
            before = iamClient.getUser(alert.getScalewayUserId());
        } catch (ScalewayApiException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, exception.getSafeMessage());
        }
        boolean lock = request.action() == RemediationAction.LOCK_USER;
        RemediationEntity entity = new RemediationEntity(
                alert.getId(),
                request.action(),
                before.id() == null ? alert.getScalewayUserId() : before.id(),
                before.username() == null ? alert.getUsername() : before.username(),
                AuthenticationContext.currentUserId(),
                AuthenticationContext.currentUsername(),
                before.locked(),
                Instant.now()
        );

        if (lock && before.locked()) {
            entity.succeed(true, "User is already locked", Instant.now());
            return finish(alert, entity);
        }
        if (!lock && !before.locked()) {
            entity.succeed(false, "User is already unlocked", Instant.now());
            return finish(alert, entity);
        }

        try {
            if (lock) {
                iamClient.lockUser(entity.getTargetUserId());
            } else {
                iamClient.unlockUser(entity.getTargetUserId());
            }
            ScalewayUserDto after = iamClient.getUser(entity.getTargetUserId());
            boolean applied = lock ? after.locked() : !after.locked();
            if (!applied) {
                entity.fail(lock
                        ? "Scaleway did not report the user as locked"
                        : "Scaleway did not report the user as unlocked", Instant.now());
            } else {
                entity.succeed(after.locked(), lock ? "User locked successfully" : "User unlocked successfully", Instant.now());
            }
        } catch (ScalewayApiException exception) {
            entity.fail(exception.getSafeMessage(), Instant.now());
        }
        return finish(alert, entity);
    }

    private RemediationResponse finish(AlertEntity alert, RemediationEntity entity) {
        RemediationEntity saved = remediations.save(entity);
        if (saved.getStatus() == RemediationStatus.SUCCESS) {
            alert.setScalewayUserLocked(saved.isAfterLocked());
            alerts.save(alert);
        }
        try {
            scalewayService.syncEvents();
        } catch (RuntimeException exception) {
            // The lock change already happened. A failed audit sync must not fail the admin session.
        }
        return RemediationResponse.from(saved);
    }

    public List<RemediationResponse> history(UUID alertId) {
        if (!alerts.existsById(alertId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found");
        }
        return remediations.findByAlertIdOrderByCreatedAtDesc(alertId).stream()
                .map(RemediationResponse::from)
                .toList();
    }
}
