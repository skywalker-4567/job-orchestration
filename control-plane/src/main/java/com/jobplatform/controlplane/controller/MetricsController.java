package com.jobplatform.controlplane.controller;

import com.jobplatform.controlplane.dto.ExecutionMetricsResponse;
import com.jobplatform.controlplane.dto.JobMetricsResponse;
import com.jobplatform.controlplane.service.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/jobs")
    public JobMetricsResponse getJobMetrics() {
        return metricsService.getJobMetrics();
    }

    @GetMapping("/executions")
    public ExecutionMetricsResponse getExecutionMetrics() {
        return metricsService.getExecutionMetrics();
    }
}