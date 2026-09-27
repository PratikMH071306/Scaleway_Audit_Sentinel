package com.secure_monitor.scaleway;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ScalewayClientTest {

    @Test
    void requestsEventsFromTheLookbackWindow() {
        ScalewayConfig config = new ScalewayConfig(
                "secret",
                "access",
                "org-1",
                "fr-par",
                Duration.ofDays(7)
        );

        URI uri = ScalewayClient.eventsUri(config, Instant.parse("2026-09-27T17:00:00Z"));

        assertThat(uri.getPath()).isEqualTo("/audit-trail/v1alpha1/regions/fr-par/events");
        assertThat(uri.getQuery()).contains("organization_id=org-1");
        assertThat(uri.getQuery()).contains("recorded_after=2026-09-20T17:00:00Z");
        assertThat(uri.getQuery()).contains("page_size=100");
    }
}
