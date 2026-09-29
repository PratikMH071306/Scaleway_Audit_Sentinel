package com.secure_monitor.remediation;

import jakarta.validation.constraints.NotNull;

public record RemediationRequest(
        @NotNull RemediationAction action
) {
}
