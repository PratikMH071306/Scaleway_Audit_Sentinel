package com.secure_monitor.scaleway.iam;

import com.secure_monitor.scaleway.ScalewayConfig;
import com.secure_monitor.scaleway.dto.ScalewayUserDto;
import com.secure_monitor.scaleway.dto.ScalewayUsersResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class ScalewayIamClient {

    private final RestClient restClient;

    public ScalewayIamClient(ScalewayConfig config) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.scaleway.com")
                .defaultHeader("X-Auth-Token", config.secretKey())
                .build();
    }

    public List<ScalewayUserDto> listUsers(String organizationId) {
        try {
            ScalewayUsersResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/iam/v1alpha1/users")
                            .queryParam("organization_id", organizationId)
                            .queryParam("page_size", 100)
                            .build())
                    .retrieve()
                    .body(ScalewayUsersResponse.class);
            if (response == null || response.users() == null) {
                return List.of();
            }
            return response.users();
        } catch (RestClientResponseException exception) {
            throw map(exception);
        } catch (RestClientException exception) {
            throw new ScalewayApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Scaleway could not be reached.",
                    exception
            );
        }
    }

    public ScalewayUserDto getUser(String userId) {
        try {
            ScalewayUserDto user = restClient.get()
                    .uri("/iam/v1alpha1/users/{userId}", userId)
                    .retrieve()
                    .body(ScalewayUserDto.class);
            if (user == null) {
                throw new ScalewayApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Scaleway returned an empty user response.",
                        null
                );
            }
            return user;
        } catch (RestClientResponseException exception) {
            throw map(exception);
        } catch (RestClientException exception) {
            throw new ScalewayApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Scaleway could not be reached.",
                    exception
            );
        }
    }

    public ScalewayUserDto lockUser(String userId) {
        try {
            ScalewayUserDto user = restClient.post()
                    .uri("/iam/v1alpha1/users/{userId}/lock", userId)
                    .body(Map.of())
                    .retrieve()
                    .body(ScalewayUserDto.class);
            if (user == null) {
                throw new ScalewayApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Scaleway returned an empty lock response.",
                        null
                );
            }
            return user;
        } catch (RestClientResponseException exception) {
            throw map(exception);
        } catch (RestClientException exception) {
            throw new ScalewayApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Scaleway could not be reached.",
                    exception
            );
        }
    }

    public ScalewayUserDto unlockUser(String userId) {
        try {
            ScalewayUserDto user = restClient.post()
                    .uri("/iam/v1alpha1/users/{userId}/unlock", userId)
                    .body(Map.of())
                    .retrieve()
                    .body(ScalewayUserDto.class);
            if (user == null) {
                throw new ScalewayApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Scaleway returned an empty unlock response.",
                        null
                );
            }
            return user;
        } catch (RestClientResponseException exception) {
            throw map(exception);
        } catch (RestClientException exception) {
            throw new ScalewayApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Scaleway could not be reached.",
                    exception
            );
        }
    }

    public static ScalewayApiException map(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String message;
        if (status.value() == 401) {
            message = "Scaleway credentials are invalid.";
        } else if (status.value() == 403) {
            message = "The application is not allowed to change this Scaleway user.";
        } else if (status.value() == 404) {
            message = "The Scaleway user was not found.";
        } else if (status.value() == 429) {
            message = "Scaleway rate limited the request. Try again shortly.";
        } else if (status.is5xxServerError()) {
            message = "Scaleway is temporarily unavailable.";
        } else {
            message = "Scaleway rejected the request.";
        }
        return new ScalewayApiException(status, message, exception);
    }
}
