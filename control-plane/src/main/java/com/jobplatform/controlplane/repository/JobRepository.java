package com.jobplatform.controlplane.repository;

import com.jobplatform.controlplane.entity.Job;
import com.jobplatform.controlplane.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    long countByStatus(JobStatus status);
}