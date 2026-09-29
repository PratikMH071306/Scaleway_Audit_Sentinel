package com.secure_monitor.repository;

import com.secure_monitor.domain.RemediationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RemediationRepository extends JpaRepository<RemediationEntity, UUID> {

    List<RemediationEntity> findByAlertIdOrderByCreatedAtDesc(UUID alertId);
}
