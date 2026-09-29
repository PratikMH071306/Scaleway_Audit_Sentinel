package com.secure_monitor.scaleway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScalewayUsersResponse(
        List<ScalewayUserDto> users
) {
}
