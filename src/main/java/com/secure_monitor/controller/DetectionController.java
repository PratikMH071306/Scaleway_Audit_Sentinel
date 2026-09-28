package com.secure_monitor.controller;

import com.secure_monitor.detection.DetectionResult;
import com.secure_monitor.detection.DetectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {

    private final DetectionService detectionService;

    public DetectionController(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @GetMapping("/failed-authentication")
    public List<DetectionResult> failedAuthentication() {
        return detectionService.detectFailedAuthentication();
    }
}
