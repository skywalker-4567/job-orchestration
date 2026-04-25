package com.jobplatform.controlplane.dto;

import com.jobplatform.controlplane.enums.JobStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public class JobResponse {
    private UUID jobId;
    private JobStatus status;
    private OffsetDateTime createdAt;

    public JobResponse(UUID jobId, JobStatus status, OffsetDateTime createdAt) {
        this.jobId = jobId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getJobId() { return jobId; }
    public JobStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}