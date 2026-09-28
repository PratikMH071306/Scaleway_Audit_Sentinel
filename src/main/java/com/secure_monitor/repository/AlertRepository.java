package com.secure_monitor.repository;

import com.secure_monitor.alert.AlertStatus;
import com.secure_monitor.domain.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

    boolean existsByRuleAndSourceIpAndStatus(String rule, String sourceIp, AlertStatus status);

    Optional<AlertEntity> findFirstByRuleAndSourceIpAndStatus(String rule, String sourceIp, AlertStatus status);
}
