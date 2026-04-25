package com.jobplatform.controlplane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.controlplane.assignment.RegionAssignmentService;
import com.jobplatform.controlplane.dto.JobExecutionResponse;
import com.jobplatform.controlplane.dto.JobRequest;
import com.jobplatform.controlplane.dto.JobResponse;
import com.jobplatform.controlplane.entity.Job;
import com.jobplatform.controlplane.entity.JobExecution;
import com.jobplatform.controlplane.entity.OutboxEvent;
import com.jobplatform.controlplane.enums.ExecutionStatus;
import com.jobplatform.controlplane.enums.JobStatus;
import com.jobplatform.controlplane.enums.Region;
import com.jobplatform.controlplane.producer.JobExecutionMessage;
import com.jobplatform.controlplane.repository.JobExecutionRepository;
import com.jobplatform.controlplane.repository.JobRepository;
import com.jobplatform.controlplane.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final RegionAssignmentService regionAssignmentService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public JobService(
            JobRepository jobRepository,
            JobExecutionRepository jobExecutionRepository,
            RegionAssignmentService regionAssignmentService,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.jobExecutionRepository = jobExecutionRepository;
        this.regionAssignmentService = regionAssignmentService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JobResponse createJob(JobRequest request) {
        if (request.getTaskType() == null || request.getTaskType().isBlank()) {
            throw new IllegalArgumentException("taskType is required");
        }
        if (request.getPayload() == null) {
            throw new IllegalArgumentException("payload is required");
        }
        if (request.getPriority() == null) {
            throw new IllegalArgumentException("priority is required");
        }

        Job job = new Job();
        job.setTaskType(request.getTaskType());
        job.setPayload(request.getPayload());
        job.setStatus(JobStatus.SUBMITTED);
        job.setPriority(request.getPriority());
        job.setMaxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 3);
        job.setTimeoutSeconds(request.getTimeoutSeconds());
        Job savedJob = jobRepository.save(job);

        log.info("Job created: jobId={}, taskType={}, priority={}",
                savedJob.getId(), savedJob.getTaskType(), savedJob.getPriority());

        Region region = regionAssignmentService.assignNextRegion();

        JobExecution execution = new JobExecution();
        execution.setJob(savedJob);
        execution.setRegion(region);
        execution.setStatus(ExecutionStatus.ASSIGNED);
        execution.setAttemptNumber(1);
        execution.setAssignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        execution.setEventId(UUID.randomUUID());
        JobExecution savedExecution = jobExecutionRepository.save(execution);

        log.info("Dispatching execution: executionId={}, region={}",
                savedExecution.getId(), region);

        writeOutboxEvent(savedExecution, savedJob);

        return toResponse(savedJob);
    }

    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public JobResponse getJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }

    @Transactional
    public List<JobExecutionResponse> getExecutionsForJob(UUID jobId) {
        return jobExecutionRepository.findByJob_Id(jobId)
                .stream()
                .map(JobExecutionResponse::from)
                .collect(Collectors.toList());
    }

    void writeOutboxEvent(JobExecution execution, Job job) {
        try {
            JobExecutionMessage message = new JobExecutionMessage(
                    execution.getId(),
                    job.getId(),
                    job.getTaskType(),
                    job.getPayload(),
                    execution.getAttemptNumber(),
                    execution.getRegion().name(),
                    execution.getEventId()
            );

            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("JOB_EXECUTION");
            event.setAggregateId(execution.getId());
            event.setEventType("EXECUTION_ASSIGNED");
            event.setPayload(objectMapper.writeValueAsString(message));
            event.setPublished(false);
            outboxEventRepository.save(event);

        } catch (Exception e) {
            throw new RuntimeException("Failed to write outbox event for execution: "
                    + execution.getId(), e);
        }
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(job.getId(), job.getStatus(), job.getCreatedAt());
    }
}