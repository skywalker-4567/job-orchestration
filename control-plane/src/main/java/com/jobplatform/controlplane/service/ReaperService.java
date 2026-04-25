package com.jobplatform.controlplane.service;

import com.jobplatform.controlplane.entity.JobExecution;
import com.jobplatform.controlplane.enums.ExecutionStatus;
import com.jobplatform.controlplane.repository.JobExecutionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ReaperService {

    private final JobExecutionRepository jobExecutionRepository;
    private final ExecutionService executionService;

    public ReaperService(
            JobExecutionRepository jobExecutionRepository,
            ExecutionService executionService
    ) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.executionService = executionService;
    }

    @Scheduled(fixedDelay = 5000)
    public void reapTimedOutExecutions() {
        List<JobExecution> running = jobExecutionRepository.findByStatus(ExecutionStatus.RUNNING);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (JobExecution execution : running) {
            Integer timeoutSeconds = execution.getJob().getTimeoutSeconds();

            if (timeoutSeconds == null) {
                continue;
            }

            if (execution.getStartedAt() == null) {
                continue;
            }

            boolean timedOut = execution.getStartedAt()
                    .plusSeconds(timeoutSeconds)
                    .isBefore(now);

            if (timedOut) {
                markTimeout(execution, now);
                executionService.failExecution(execution.getId(), "Execution timed out");
            }
        }
    }

    @Transactional
    public void markTimeout(JobExecution execution, OffsetDateTime now) {
        execution.setStatus(ExecutionStatus.TIMEOUT);
        execution.setCompletedAt(now);
        jobExecutionRepository.save(execution);
    }
}