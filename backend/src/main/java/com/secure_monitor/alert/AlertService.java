package com.secure_monitor.alert;

import com.secure_monitor.detection.DetectionResult;
import com.secure_monitor.detection.FailedAuthenticationRule;
import com.secure_monitor.domain.AlertEntity;
import com.secure_monitor.repository.AlertRepository;
import com.secure_monitor.scaleway.iam.ScalewayUserResolver;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    static final String SUSPICIOUS_AUTHENTICATION_TITLE = "Suspicious authentication activity";

    private final AlertRepository repository;
    private final ScalewayUserResolver userResolver;

    public AlertService(AlertRepository repository, ScalewayUserResolver userResolver) {
        this.repository = repository;
        this.userResolver = userResolver;
    }

    public AlertResponse record(DetectionResult result) {
        if (!result.detected()) {
            throw new IllegalArgumentException("A detection is required to create an alert");
        }
        if (repository.existsByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                result.rule(),
                result.organizationId(),
                result.username(),
                result.sourceIp(),
                AlertStatus.OPEN
        )) {
            return repository.findFirstByRuleAndOrganizationIdAndUsernameAndSourceIpAndStatus(
                            result.rule(),
                            result.organizationId(),
                            result.username(),
                            result.sourceIp(),
                            AlertStatus.OPEN
                    )
                    .map(AlertResponse::from)
                    .orElseThrow();
        }
        return AlertResponse.from(repository.save(toEntity(result)));
    }

    public List<AlertResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt")).stream()
                .map(AlertResponse::from)
                .toList();
    }

    public AlertResponse get(UUID id) {
        AlertEntity alert = userResolver.attachUserId(find(id));
        return AlertResponse.from(userResolver.refreshLockState(alert));
    }

    public AlertResponse resolve(UUID id) {
        AlertEntity entity = find(id);
        entity.setStatus(AlertStatus.RESOLVED);
        return AlertResponse.from(repository.save(entity));
    }

    private AlertEntity find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static AlertEntity toEntity(DetectionResult result) {
        return new AlertEntity(
                result.rule(),
                severityFor(result),
                AlertStatus.OPEN,
                titleFor(result.rule()),
                result.description(),
                result.detectedAt(),
                result.sourceIp(),
                result.organizationId(),
                result.username(),
                result.scalewayUserId(),
                result.eventCount(),
                result.relatedEventIds() == null ? List.of() : List.copyOf(result.relatedEventIds())
        );
    }

    private static AlertSeverity severityFor(String rule) {
        if (FailedAuthenticationRule.RULE_NAME.equals(rule)) {
            return AlertSeverity.HIGH;
        }
        return AlertSeverity.MEDIUM;
    }

    private static AlertSeverity severityFor(DetectionResult result) {
        if (FailedAuthenticationRule.RULE_NAME.equals(result.rule()) && result.followedBySuccess()) {
            return AlertSeverity.CRITICAL;
        }
        return severityFor(result.rule());
    }

    private static String titleFor(String rule) {
        if (FailedAuthenticationRule.RULE_NAME.equals(rule)) {
            return SUSPICIOUS_AUTHENTICATION_TITLE;
        }
        return rule;
    }
}
