package com.jobplatform.controlplane.service;

import com.jobplatform.controlplane.dto.ExecutionMetricsResponse;
import com.jobplatform.controlplane.dto.JobMetricsResponse;
import com.jobplatform.controlplane.entity.JobExecution;
import com.jobplatform.controlplane.enums.ExecutionStatus;
import com.jobplatform.controlplane.enums.JobStatus;
import com.jobplatform.controlplane.repository.JobExecutionRepository;
import com.jobplatform.controlplane.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class MetricsService {

    private final JobRepository jobRepository;
    private final JobExecutionRepository jobExecutionRepository;

    public MetricsService(
            JobRepository jobRepository,
            JobExecutionRepository jobExecutionRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobExecutionRepository = jobExecutionRepository;
    }

    public JobMetricsResponse getJobMetrics() {
        long total = jobRepository.count();
        long completed = jobRepository.countByStatus(JobStatus.COMPLETED);
        long failed = jobRepository.countByStatus(JobStatus.FAILED);
        long inProgress = jobRepository.countByStatus(JobStatus.IN_PROGRESS);

        return new JobMetricsResponse(total, completed, failed, inProgress);
    }

    @Transactional
    public ExecutionMetricsResponse getExecutionMetrics() {
        long total = jobExecutionRepository.count();
        long running = jobExecutionRepository.countByStatus(ExecutionStatus.RUNNING);
        long completed = jobExecutionRepository.countByStatus(ExecutionStatus.COMPLETED);
        long failed = jobExecutionRepository.countByStatus(ExecutionStatus.FAILED);
        long timeout = jobExecutionRepository.countByStatus(ExecutionStatus.TIMEOUT);

        List<JobExecution> completedExecutions =
                jobExecutionRepository.findByStatus(ExecutionStatus.COMPLETED);

        double avgMs = completedExecutions.stream()
                .filter(e -> e.getStartedAt() != null && e.getCompletedAt() != null)
                .mapToLong(e -> Duration.between(e.getStartedAt(), e.getCompletedAt()).toMillis())
                .average()
                .orElse(0.0);

        return new ExecutionMetricsResponse(total, running, completed, failed, timeout, avgMs);
    }
}