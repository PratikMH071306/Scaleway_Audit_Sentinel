package com.secure_monitor.controller;

import com.secure_monitor.alert.AlertResponse;
import com.secure_monitor.alert.AlertService;
import com.secure_monitor.alert.AlertSeverity;
import com.secure_monitor.alert.AlertStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    @Test
    void listsStoredAlerts() throws Exception {
        UUID id = UUID.fromString("8e5c0000-0000-4000-8000-000000000001");
        when(alertService.list()).thenReturn(List.of(sample(id, "122.167.118.87")));

        mockMvc().perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].rule").value("FAILED_AUTHENTICATION_SPIKE"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].title").value("Multiple failed authentication attempts"))
                .andExpect(jsonPath("$[0].description").value("5 failed authentication attempts detected within 5 minutes"))
                .andExpect(jsonPath("$[0].detectedAt").value("2026-09-28T10:30:00Z"))
                .andExpect(jsonPath("$[0].sourceIp").value("122.167.118.87"))
                .andExpect(jsonPath("$[0].eventCount").value(5))
                .andExpect(jsonPath("$[0].relatedEventIds[0]").value("e1"));
    }

    @Test
    void returnsOneAlertById() throws Exception {
        UUID id = UUID.fromString("8e5c0000-0000-4000-8000-000000000001");
        when(alertService.get(id)).thenReturn(sample(id, "122.167.118.87"));

        mockMvc().perform(get("/api/alerts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.rule").value("FAILED_AUTHENTICATION_SPIKE"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.sourceIp").value("122.167.118.87"))
                .andExpect(jsonPath("$.eventCount").value(5));
    }

    @Test
    void returnsNotFoundForAnUnknownAlert() throws Exception {
        UUID id = UUID.fromString("8e5c0000-0000-4000-8000-000000000099");
        when(alertService.get(id)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc().perform(get("/api/alerts/{id}", id))
                .andExpect(status().isNotFound());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new AlertController(alertService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    private static AlertResponse sample(UUID id, String sourceIp) {
        return new AlertResponse(
                id,
                "FAILED_AUTHENTICATION_SPIKE",
                AlertSeverity.HIGH,
                AlertStatus.OPEN,
                "Multiple failed authentication attempts",
                "5 failed authentication attempts detected within 5 minutes",
                Instant.parse("2026-09-28T10:30:00Z"),
                sourceIp,
                5,
                List.of("e1", "e2", "e3", "e4", "e5")
        );
    }
}
