package com.secure_monitor.scaleway;

import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import com.secure_monitor.scaleway.dto.AuditEventDto;
import com.secure_monitor.scaleway.dto.AuditEventsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScalewayServiceTest {

    @Mock
    private ScalewayClient client;

    @Mock
    private SecurityEventRepository repository;

    private final ScalewayEventMapper mapper = new ScalewayEventMapper();

    @Test
    void savesMappedEvents() {
        AuditEventDto event = event("evt-1", 201);
        when(client.getEvents()).thenReturn(new AuditEventsResponse(List.of(event)));
        when(repository.findAllById(any())).thenReturn(List.of());

        service().syncEvents();

        ArgumentCaptor<Iterable<SecurityEventEntity>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(saved.capture());
        SecurityEventEntity entity = saved.getValue().iterator().next();
        assertThat(entity.getEventId()).isEqualTo("evt-1");
        assertThat(entity.getOrganizationId()).isEqualTo("org-1");
        assertThat(entity.getStatusCode()).isEqualTo(201);
    }

    @Test
    void skipsEventsWithoutAnId() {
        when(client.getEvents()).thenReturn(new AuditEventsResponse(List.of(event("  ", 500))));

        service().syncEvents();

        verifyNoInteractions(repository);
    }

    @Test
    void doesNotSaveWhenResponseHasNoEvents() {
        when(client.getEvents()).thenReturn(new AuditEventsResponse(null));

        service().syncEvents();

        verifyNoInteractions(repository);
    }

    @Test
    void doesNotInsertTheSameEventTwice() {
        when(client.getEvents()).thenReturn(new AuditEventsResponse(List.of(event("evt-1", 201))));
        List<SecurityEventEntity> stored = new ArrayList<>();
        when(repository.findAllById(any())).thenAnswer(invocation -> {
            Set<String> ids = new HashSet<>();
            Iterable<String> requested = invocation.getArgument(0);
            requested.forEach(ids::add);
            return stored.stream()
                    .filter(entity -> ids.contains(entity.getEventId()))
                    .toList();
        });
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            List<SecurityEventEntity> batch = new ArrayList<>();
            Iterable<SecurityEventEntity> requested = invocation.getArgument(0);
            requested.forEach(batch::add);
            stored.addAll(batch);
            return batch;
        });

        ScalewayService service = service();
        service.syncEvents();
        service.syncEvents();

        verify(repository, times(1)).saveAll(any());
        assertThat(stored).extracting(SecurityEventEntity::getEventId).containsExactly("evt-1");
    }

    @Test
    void savesOnlyEventsThatAreNotAlreadyStored() {
        when(client.getEvents()).thenReturn(new AuditEventsResponse(List.of(
                event("evt-1", 201),
                event("evt-2", 403)
        )));
        when(repository.findAllById(any())).thenReturn(List.of(mapper.toEntity(event("evt-1", 201))));

        service().syncEvents();

        ArgumentCaptor<Iterable<SecurityEventEntity>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(SecurityEventEntity::getEventId).containsExactly("evt-2");
    }

    private ScalewayService service() {
        return new ScalewayService(client, mapper, repository);
    }

    private static AuditEventDto event(String id, int statusCode) {
        return new AuditEventDto(
                id,
                Instant.parse("2026-09-27T12:00:00Z"),
                "fr-par",
                null,
                "org-1",
                "203.0.113.10",
                null,
                "instance",
                "instance",
                "CreateServer",
                "req-9",
                null,
                statusCode,
                null
        );
    }
}
