package com.secure_monitor.detection;

import com.secure_monitor.domain.SecurityEventEntity;

import java.util.List;

public interface DetectionRule {

    List<DetectionResult> evaluate(List<SecurityEventEntity> events);
}
