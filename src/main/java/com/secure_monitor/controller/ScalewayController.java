package com.secure_monitor.controller;


import com.secure_monitor.scaleway.ScalewayService;
import com.secure_monitor.scaleway.dto.AuditEventsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scaleway")
public class ScalewayController {

    private final ScalewayService service;

    public ScalewayController(ScalewayService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public AuditEventsResponse getEvents() {
        return service.getAuditEvents();
    }
}
