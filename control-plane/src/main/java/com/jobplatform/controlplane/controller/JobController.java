package com.jobplatform.controlplane.controller;

import com.jobplatform.controlplane.dto.JobExecutionResponse;
import com.jobplatform.controlplane.dto.JobRequest;
import com.jobplatform.controlplane.dto.JobResponse;
import com.jobplatform.controlplane.repository.JobRepository;
import com.jobplatform.controlplane.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final JobRepository jobRepository;

    public JobController(JobService jobService, JobRepository jobRepository) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse createJob(@Valid @RequestBody JobRequest request) {
        return jobService.createJob(request);
    }

    @GetMapping
    public List<JobResponse> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/{jobId}")
    public JobResponse getJob(@PathVariable UUID jobId) {
        return jobService.getJobById(jobId);
    }

    @GetMapping("/{jobId}/executions")
    public List<JobExecutionResponse> getExecutions(@PathVariable UUID jobId) {
        return jobService.getExecutionsForJob(jobId);
    }
}