package com.secure_monitor.controller;

import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecurityEventControllerTest {

    @Mock
    private SecurityEventRepository repository;

    @Test
    void listsEventsStoredInTheDatabase() throws Exception {
        SecurityEventEntity entity = new SecurityEventEntity(
                "evt-1",
                Instant.parse("2026-09-27T12:00:00Z"),
                "fr-par",
                "org-1",
                "203.0.113.10",
                "scaleway-cli",
                "instance",
                "instance",
                "CreateServer",
                "req-9",
                201
        );
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(entity));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SecurityEventController(repository)).build();

        mockMvc.perform(get("/api/security-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$[0].organizationId").value("org-1"))
                .andExpect(jsonPath("$[0].statusCode").value(201));
    }
}
