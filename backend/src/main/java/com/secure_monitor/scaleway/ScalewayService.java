package com.secure_monitor.scaleway;

import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import com.secure_monitor.scaleway.dto.AuditEventDto;
import com.secure_monitor.scaleway.dto.AuditEventsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScalewayService {

    private final ScalewayClient client;
    private final ScalewayEventMapper mapper;
    private final SecurityEventRepository repository;

    public ScalewayService(
            ScalewayClient client,
            ScalewayEventMapper mapper,
            SecurityEventRepository repository
    ) {
        this.client = client;
        this.mapper = mapper;
        this.repository = repository;
    }

    public AuditEventsResponse getAuditEvents() {
        return client.getEvents();
    }

    @Transactional
    public void syncEvents() {
        AuditEventsResponse response = client.getEvents();
        List<AuditEventDto> events = response == null || response.events() == null
                ? List.of()
                : response.events();

        Map<String, SecurityEventEntity> unique = new LinkedHashMap<>();
        int alreadySeenInBatch = 0;
        for (AuditEventDto event : events) {
            if (event == null || event.id() == null || event.id().isBlank()) {
                continue;
            }
            if (unique.containsKey(event.id())) {
                alreadySeenInBatch++;
                continue;
            }
            unique.put(event.id(), mapper.toEntity(event));
        }

        Set<String> existingIds = unique.isEmpty()
                ? Set.of()
                : repository.findAllById(unique.keySet()).stream()
                        .map(SecurityEventEntity::getEventId)
                        .collect(Collectors.toSet());

        List<SecurityEventEntity> toSave = new ArrayList<>();
        for (SecurityEventEntity entity : unique.values()) {
            if (!existingIds.contains(entity.getEventId())) {
                toSave.add(entity);
            }
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }

        int skipped = alreadySeenInBatch + (unique.size() - toSave.size());
        log.info("Fetched {} events from Scaleway", events.size());
        log.info("Saved {} new events", toSave.size());
        log.info("Skipped {} existing events", skipped);
    }
}
