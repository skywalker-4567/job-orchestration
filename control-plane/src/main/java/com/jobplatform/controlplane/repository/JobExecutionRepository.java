package com.jobplatform.controlplane.repository;

import com.jobplatform.controlplane.entity.JobExecution;
import com.jobplatform.controlplane.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

    List<JobExecution> findByJob_Id(UUID jobId);

    Optional<JobExecution> findTopByJob_IdOrderByAttemptNumberDesc(UUID jobId);

    List<JobExecution> findByStatus(ExecutionStatus status);

    long countByStatus(ExecutionStatus status);
}