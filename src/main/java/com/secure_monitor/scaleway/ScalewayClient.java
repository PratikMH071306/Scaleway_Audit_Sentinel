package com.secure_monitor.scaleway;

import com.secure_monitor.scaleway.dto.AuditEventsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ScalewayClient {

    private final RestClient restClient;
    private final ScalewayConfig config;


    public ScalewayClient(ScalewayConfig config) {

        System.out.println("Scaleway organization: " + config.organizationId());
        System.out.println("Scaleway region: " + config.region());
        System.out.println("Secret key present: "
                + (config.secretKey() != null && !config.secretKey().isBlank()));

        this.config = config;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.scaleway.com")
                .defaultHeader(
                        "X-Auth-Token",
                        config.secretKey()
                )
                .build();
    }


    public AuditEventsResponse getEvents() {
        AuditEventsResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/audit-trail/v1alpha1/regions/{region}/events")
                        .queryParam("organization_id", config.organizationId())
                        .build(config.region())
                )
                .retrieve()
                .body(AuditEventsResponse.class);

        int eventCount = response == null || response.events() == null
                ? 0
                : response.events().size();
        log.info("Fetched {} events from Scaleway region {}", eventCount, config.region());

        return response;
    }
}