package com.jobplatform.controlplane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.controlplane.entity.Job;
import com.jobplatform.controlplane.entity.JobExecution;
import com.jobplatform.controlplane.entity.OutboxEvent;
import com.jobplatform.controlplane.enums.ExecutionStatus;
import com.jobplatform.controlplane.enums.JobStatus;
import com.jobplatform.controlplane.enums.Region;
import com.jobplatform.controlplane.producer.DLQProducer;
import com.jobplatform.controlplane.producer.JobDLQMessage;
import com.jobplatform.controlplane.producer.JobExecutionMessage;
import com.jobplatform.controlplane.repository.JobExecutionRepository;
import com.jobplatform.controlplane.repository.JobRepository;
import com.jobplatform.controlplane.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final JobExecutionRepository jobExecutionRepository;
    private final JobRepository jobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DLQProducer dlqProducer;
    private final ObjectMapper objectMapper;

    public ExecutionService(
            JobExecutionRepository jobExecutionRepository,
            JobRepository jobRepository,
            OutboxEventRepository outboxEventRepository,
            DLQProducer dlqProducer,
            ObjectMapper objectMapper
    ) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobRepository = jobRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.dlqProducer = dlqProducer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void startExecution(UUID executionId, String workerId) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId is required");
        }

        JobExecution execution = jobExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() != ExecutionStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Cannot start execution in status: " + execution.getStatus());
        }

        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setWorkerId(workerId);
        execution.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            jobExecutionRepository.save(execution);
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("Concurrent update detected for execution: " + executionId);
        }

        log.info("Execution started: executionId={}, attempt={}, workerId={}",
                executionId, execution.getAttemptNumber(), workerId);

        Job job = execution.getJob();
        if (job.getStatus() != JobStatus.COMPLETED) {
            job.setStatus(JobStatus.IN_PROGRESS);
            try {
                jobRepository.save(job);
            } catch (OptimisticLockingFailureException ex) {
                throw new IllegalStateException("Concurrent update detected for job: " + job.getId());
            }
        }
    }

    @Transactional
    public void completeExecution(UUID executionId) {
        JobExecution execution = jobExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot complete execution in status: " + execution.getStatus());
        }

        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            jobExecutionRepository.save(execution);
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("Concurrent update detected for execution: " + executionId);
        }

        log.info("Execution completed: executionId={}, attempt={}",
                executionId, execution.getAttemptNumber());

        Job job = execution.getJob();
        job.setStatus(JobStatus.COMPLETED);
        try {
            jobRepository.save(job);
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("Concurrent update detected for job: " + job.getId());
        }
    }

    @Transactional
    public void failExecution(UUID executionId, String errorMessage) {
        if (errorMessage != null && errorMessage.isBlank()) {
            errorMessage = null;
        }

        JobExecution execution = jobExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() != ExecutionStatus.RUNNING
                && execution.getStatus() != ExecutionStatus.TIMEOUT) {
            throw new IllegalStateException(
                    "Cannot fail execution in status: " + execution.getStatus());
        }

        if (execution.getStatus() != ExecutionStatus.TIMEOUT) {
            execution.setStatus(ExecutionStatus.FAILED);
        }
        execution.setErrorMessage(errorMessage);
        execution.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            jobExecutionRepository.save(execution);
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("Concurrent update detected for execution: " + executionId);
        }

        log.info("Execution failed: executionId={}, attempt={}, reason={}",
                executionId, execution.getAttemptNumber(), errorMessage);

        Job job = execution.getJob();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        JobExecution latest = jobExecutionRepository
                .findTopByJob_IdOrderByAttemptNumberDesc(job.getId())
                .orElseThrow();

        if (!latest.getId().equals(execution.getId())) {
            return;
        }

        List<JobExecution> allExecutions = jobExecutionRepository.findByJob_Id(job.getId());

        boolean allTerminalAndFailed = allExecutions.stream()
                .allMatch(e -> e.getStatus() == ExecutionStatus.FAILED
                        || e.getStatus() == ExecutionStatus.TIMEOUT);

        boolean attemptsExhausted = allExecutions.size() >= job.getMaxAttempts();

        if (allTerminalAndFailed && attemptsExhausted) {
            job.setStatus(JobStatus.FAILED);
            try {
                jobRepository.save(job);
            } catch (OptimisticLockingFailureException ex) {
                throw new IllegalStateException("Concurrent update detected for job: " + job.getId());
            }

            log.info("Job moved to DLQ: jobId={}", job.getId());

            dlqProducer.publishDLQ(new JobDLQMessage(
                    job.getId(),
                    execution.getId(),
                    job.getTaskType(),
                    job.getPayload(),
                    execution.getAttemptNumber(),
                    execution.getRegion().name(),
                    execution.getErrorMessage(),
                    UUID.randomUUID(),
                    now
            ));

        } else if (!attemptsExhausted) {
            if (job.getStatus() != JobStatus.COMPLETED) {
                job.setStatus(JobStatus.IN_PROGRESS);
                try {
                    jobRepository.save(job);
                } catch (OptimisticLockingFailureException ex) {
                    throw new IllegalStateException("Concurrent update detected for job: " + job.getId());
                }
            }
            log.info("Retrying execution: jobId={}, attempt={}",
                    job.getId(), execution.getAttemptNumber() + 1);
            createRetryExecution(job, execution);
        }
    }

    private void createRetryExecution(Job job, JobExecution previousExecution) {
        int nextAttempt = previousExecution.getAttemptNumber() + 1;
        Region region = previousExecution.getRegion();

        JobExecution retry = new JobExecution();
        retry.setJob(job);
        retry.setRegion(region);
        retry.setStatus(ExecutionStatus.ASSIGNED);
        retry.setAttemptNumber(nextAttempt);
        retry.setAssignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        retry.setEventId(UUID.randomUUID());
        JobExecution saved = jobExecutionRepository.save(retry);

        log.info("Dispatching retry execution: executionId={}, region={}",
                saved.getId(), region);

        writeOutboxEvent(saved, job);
    }

    private void writeOutboxEvent(JobExecution execution, Job job) {
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
}