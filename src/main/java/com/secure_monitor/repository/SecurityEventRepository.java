package com.secure_monitor.repository;

import com.secure_monitor.domain.SecurityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, String> {
}
