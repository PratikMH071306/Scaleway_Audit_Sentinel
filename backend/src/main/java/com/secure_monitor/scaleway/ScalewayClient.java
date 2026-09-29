package com.secure_monitor.scaleway;

import com.secure_monitor.scaleway.dto.AuditEventsResponse;
import com.secure_monitor.scaleway.iam.ScalewayApiException;
import com.secure_monitor.scaleway.iam.ScalewayIamClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

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
        AuditEventsResponse response;
        try {
            response = restClient.get()
                    .uri(eventsUri(config, Instant.now()).toString())
                    .retrieve()
                    .body(AuditEventsResponse.class);
        } catch (RestClientResponseException exception) {
            throw ScalewayIamClient.map(exception);
        } catch (RestClientException exception) {
            throw new ScalewayApiException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Scaleway could not be reached.",
                    exception
            );
        }

        int eventCount = response == null || response.events() == null
                ? 0
                : response.events().size();
        log.info("Fetched {} events from Scaleway region {}", eventCount, config.region());

        return response;
    }

    static URI eventsUri(ScalewayConfig config, Instant now) {
        Instant recordedAfter = now.minus(config.eventLookback());
        return UriComponentsBuilder
                .fromPath("/audit-trail/v1alpha1/regions/{region}/events")
                .queryParam("organization_id", config.organizationId())
                .queryParam("recorded_after", recordedAfter)
                .queryParam("page_size", 100)
                .build(config.region());
    }
}