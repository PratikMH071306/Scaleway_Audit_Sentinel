package com.secure_monitor.controller;

import com.secure_monitor.domain.SecurityEventEntity;
import com.secure_monitor.repository.SecurityEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/security-events")
public class SecurityEventController {

    private final SecurityEventRepository repository;

    public SecurityEventController(SecurityEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<SecurityEventEntity> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "recordedAt"));
    }
}
