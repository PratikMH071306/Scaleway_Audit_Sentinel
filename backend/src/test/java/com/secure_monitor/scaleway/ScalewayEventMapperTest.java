package com.secure_monitor.scaleway;

import com.secure_monitor.domain.SecurityEvent;
import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.scaleway.dto.AuditEventDto;
import com.secure_monitor.scaleway.dto.PrincipalDto;
import com.secure_monitor.scaleway.dto.ResourceDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScalewayEventMapperTest {

    private final ScalewayEventMapper mapper = new ScalewayEventMapper();

    @Test
    void mapsCompleteAuditEvent() {
        Instant recordedAt = Instant.parse("2026-09-27T12:00:00Z");
        AuditEventDto event = event(
                "evt-1",
                recordedAt,
                new PrincipalDto("user-42"),
                "203.0.113.10",
                "instance",
                "CreateServer",
                201,
                List.of(
                        resource("srv-1", "instance_server", "web"),
                        resource("ip-9", "flexible_ip", null)
                )
        );

        SecurityEvent mapped = mapper.map(event);

        assertThat(mapped).isEqualTo(new SecurityEvent(
                "evt-1",
                recordedAt,
                "user-42",
                null,
                "203.0.113.10",
                "instance.CreateServer",
                "instance_server/web, flexible_ip/ip-9",
                "success"
        ));
    }

    @Test
    void mapsMissingPrincipalAndResources() {
        SecurityEvent mapped = mapper.map(event(
                "evt-2",
                Instant.parse("2026-09-27T13:00:00Z"),
                null,
                null,
                "iam",
                "  ",
                500,
                null
        ));

        assertThat(mapped.principalId()).isNull();
        assertThat(mapped.principalType()).isNull();
        assertThat(mapped.action()).isEqualTo("iam");
        assertThat(mapped.resource()).isNull();
        assertThat(mapped.status()).isEqualTo("failure");
    }

    @Test
    void usesMethodNameWhenServiceNameIsBlank() {
        SecurityEvent mapped = mapper.map(event(
                "evt-3",
                Instant.EPOCH,
                new PrincipalDto(null),
                "10.0.0.1",
                "",
                "DeleteVolume",
                404,
                List.of()
        ));

        assertThat(mapped.principalId()).isNull();
        assertThat(mapped.action()).isEqualTo("DeleteVolume");
        assertThat(mapped.resource()).isNull();
        assertThat(mapped.status()).isEqualTo("failure");
    }

    @Test
    void formatsResourcesByTypeNameAndId() {
        SecurityEvent mapped = mapper.map(event(
                "evt-4",
                Instant.EPOCH,
                new PrincipalDto("user-1"),
                null,
                "registry",
                "GetImage",
                200,
                Arrays.asList(
                        resource(null, "namespace", "prod"),
                        resource("img-7", null, "   "),
                        resource(null, "secret", null),
                        null,
                        resource("   ", "   ", "  ")
                )
        ));

        assertThat(mapped.resource()).isEqualTo("namespace/prod, img-7, secret");
    }

    @ParameterizedTest
    @CsvSource({
            "200, success",
            "299, success",
            "401, forbidden",
            "403, forbidden",
            "400, failure",
            "500, failure"
    })
    void mapsStatusFromStatusCode(int statusCode, String expectedStatus) {
        SecurityEvent mapped = mapper.map(event(
                "evt-status",
                Instant.EPOCH,
                null,
                null,
                "instance",
                "ListServers",
                statusCode,
                null
        ));

        assertThat(mapped.status()).isEqualTo(expectedStatus);
    }

    @Test
    void mapsAuditEventToEntity() {
        Instant recordedAt = Instant.parse("2026-09-27T12:00:00Z");
        AuditEventDto event = new AuditEventDto(
                "evt-1",
                recordedAt,
                "fr-par",
                new PrincipalDto("user-42"),
                "org-1",
                "203.0.113.10",
                "scaleway-cli",
                "instance",
                "instance",
                "CreateServer",
                "req-9",
                Map.of("name", "web"),
                201,
                List.of(resource("srv-1", "instance_server", "web"))
        );

        SecurityEventEntity entity = mapper.toEntity(event);

        assertThat(entity.getEventId()).isEqualTo("evt-1");
        assertThat(entity.getRecordedAt()).isEqualTo(recordedAt);
        assertThat(entity.getLocality()).isEqualTo("fr-par");
        assertThat(entity.getOrganizationId()).isEqualTo("org-1");
        assertThat(entity.getSourceIp()).isEqualTo("203.0.113.10");
        assertThat(entity.getUserAgent()).isEqualTo("scaleway-cli");
        assertThat(entity.getProductName()).isEqualTo("instance");
        assertThat(entity.getServiceName()).isEqualTo("instance");
        assertThat(entity.getMethodName()).isEqualTo("CreateServer");
        assertThat(entity.getRequestId()).isEqualTo("req-9");
        assertThat(entity.getRequestBody()).isEqualTo(Map.of("name", "web"));
        assertThat(entity.getStatusCode()).isEqualTo(201);
        assertThat(entity.getResources()).containsExactly(Map.of(
                "id", "srv-1",
                "type", "instance_server",
                "name", "web"
        ));
    }

    private static AuditEventDto event(
            String id,
            Instant recordedAt,
            PrincipalDto principal,
            String sourceIp,
            String serviceName,
            String methodName,
            int statusCode,
            List<ResourceDto> resources
    ) {
        return new AuditEventDto(
                id,
                recordedAt,
                null,
                principal,
                null,
                sourceIp,
                null,
                null,
                serviceName,
                methodName,
                null,
                null,
                statusCode,
                resources
        );
    }

    private static ResourceDto resource(String id, String type, String name) {
        return new ResourceDto(id, type, null, null, null, name);
    }
}
