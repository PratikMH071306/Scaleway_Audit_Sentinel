package com.secure_monitor.controller;

import com.secure_monitor.alert.AlertResponse;
import com.secure_monitor.alert.AlertService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> list() {
        return alertService.list();
    }

    @GetMapping("/{id}")
    public AlertResponse get(@PathVariable UUID id) {
        return alertService.get(id);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public AlertResponse resolve(@PathVariable UUID id) {
        return alertService.resolve(id);
    }
}
