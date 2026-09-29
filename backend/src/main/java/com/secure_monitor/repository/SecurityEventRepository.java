package com.secure_monitor.repository;

import com.secure_monitor.domain.SecurityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, String> {

    List<SecurityEventEntity> findByServiceNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
            String serviceName,
            Instant recordedAt
    );
}
