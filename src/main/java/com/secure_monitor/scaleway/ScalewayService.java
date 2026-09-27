package com.secure_monitor.scaleway;

import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import com.secure_monitor.scaleway.dto.AuditEventDto;
import com.secure_monitor.scaleway.dto.AuditEventsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public AuditEventsResponse getAuditEvents() {
        AuditEventsResponse response = client.getEvents();
        List<AuditEventDto> events = response == null ? null : response.events();
        if (events == null || events.isEmpty()) {
            return response;
        }

        List<SecurityEventEntity> entities = events.stream()
                .filter(event -> event != null && event.id() != null && !event.id().isBlank())
                .map(mapper::toEntity)
                .toList();
        repository.saveAll(entities);
        return response;
    }
}
