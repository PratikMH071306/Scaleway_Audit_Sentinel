package com.secure_monitor.detection;

import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class DetectionService {

    static final Duration RECENT_EVENT_LOOKBACK = Duration.ofDays(7);

    private final SecurityEventRepository repository;
    private final FailedAuthenticationRule failedAuthenticationRule;

    public DetectionService(
            SecurityEventRepository repository,
            FailedAuthenticationRule failedAuthenticationRule
    ) {
        this.repository = repository;
        this.failedAuthenticationRule = failedAuthenticationRule;
    }

    public List<DetectionResult> detectFailedAuthentication() {
        Instant since = Instant.now().minus(RECENT_EVENT_LOOKBACK);
        List<SecurityEventEntity> events = repository
                .findByServiceNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                        FailedAuthenticationRule.AUTHENTICATION_SERVICE,
                        since
                );
        return failedAuthenticationRule.evaluate(events);
    }
}
