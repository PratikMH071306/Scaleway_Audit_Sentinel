package com.secure_monitor.scaleway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ResourceDto(
        String id,
        String type,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("updated_at")
        Instant updatedAt,

        @JsonProperty("deleted_at")
        Instant deletedAt,

        String name
) {}