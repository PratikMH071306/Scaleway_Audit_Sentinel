package com.secure_monitor.controller;

import com.secure_monitor.detection.DetectionResult;
import com.secure_monitor.detection.DetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DetectionControllerTest {

    @Mock
    private DetectionService detectionService;

    @Test
    void returnsADetectionWhenTheRuleMatches() throws Exception {
        Instant detectedAt = Instant.parse("2026-09-28T10:30:00Z");
        when(detectionService.detectFailedAuthentication()).thenReturn(List.of(
                new DetectionResult(
                        true,
                        "FAILED_AUTHENTICATION_SPIKE",
                        "5 failed authentication attempts detected within 5 minutes",
                        5,
                        "122.167.118.87",
                        detectedAt,
                        List.of("auth-1", "auth-2", "auth-3", "auth-4", "auth-5")
                ),
                new DetectionResult(
                        true,
                        "FAILED_AUTHENTICATION_SPIKE",
                        "5 failed authentication attempts detected within 5 minutes",
                        5,
                        "122.167.118.89",
                        detectedAt,
                        List.of("auth-6", "auth-7", "auth-8", "auth-9", "auth-10")
                )
        ));

        mockMvc().perform(get("/api/detection/failed-authentication"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].detected").value(true))
                .andExpect(jsonPath("$[0].sourceIp").value("122.167.118.87"))
                .andExpect(jsonPath("$[1].detected").value(true))
                .andExpect(jsonPath("$[1].rule").value("FAILED_AUTHENTICATION_SPIKE"))
                .andExpect(jsonPath("$[1].description").value("5 failed authentication attempts detected within 5 minutes"))
                .andExpect(jsonPath("$[1].eventCount").value(5))
                .andExpect(jsonPath("$[1].sourceIp").value("122.167.118.89"));
    }

    @Test
    void omitsDescriptionWhenNothingIsDetected() throws Exception {
        when(detectionService.detectFailedAuthentication()).thenReturn(List.of(new DetectionResult(
                false,
                "FAILED_AUTHENTICATION_SPIKE",
                null,
                2,
                "122.167.118.87",
                Instant.parse("2026-09-28T10:30:00Z"),
                List.of("auth-1", "auth-2")
        )));

        mockMvc().perform(get("/api/detection/failed-authentication"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].detected").value(false))
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].eventCount").value(2))
                .andExpect(jsonPath("$[0].sourceIp").value("122.167.118.87"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new DetectionController(detectionService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }
}
