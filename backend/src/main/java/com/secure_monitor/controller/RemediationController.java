package com.secure_monitor.controller;

import com.secure_monitor.remediation.RemediationRequest;
import com.secure_monitor.remediation.RemediationResponse;
import com.secure_monitor.remediation.RemediationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts/{alertId}")
public class RemediationController {

    private final RemediationService remediationService;

    public RemediationController(RemediationService remediationService) {
        this.remediationService = remediationService;
    }

    @PostMapping("/remediate")
    @PreAuthorize("hasRole('ADMIN')")
    public RemediationResponse remediate(
            @PathVariable UUID alertId,
            @Valid @RequestBody RemediationRequest request
    ) {
        return remediationService.remediate(alertId, request);
    }

    @GetMapping("/remediations")
    public List<RemediationResponse> history(@PathVariable UUID alertId) {
        return remediationService.history(alertId);
    }
}
