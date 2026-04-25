package com.jobplatform.controlplane.dto;

import com.jobplatform.controlplane.entity.JobExecution;
import com.jobplatform.controlplane.enums.ExecutionStatus;
import com.jobplatform.controlplane.enums.Region;

import java.time.OffsetDateTime;
import java.util.UUID;

public class JobExecutionResponse {

    private UUID executionId;
    private UUID jobId;
    private Region region;
    private ExecutionStatus status;
    private Integer attemptNumber;
    private String workerId;
    private UUID eventId;
    private OffsetDateTime assignedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String errorMessage;

    public static JobExecutionResponse from(JobExecution e) {
        JobExecutionResponse r = new JobExecutionResponse();
        r.executionId = e.getId();
        r.jobId = e.getJob().getId();
        r.region = e.getRegion();
        r.status = e.getStatus();
        r.attemptNumber = e.getAttemptNumber();
        r.workerId = e.getWorkerId();
        r.eventId = e.getEventId();
        r.assignedAt = e.getAssignedAt();
        r.startedAt = e.getStartedAt();
        r.completedAt = e.getCompletedAt();
        r.errorMessage = e.getErrorMessage();
        return r;
    }

    public UUID getExecutionId() { return executionId; }
    public UUID getJobId() { return jobId; }
    public Region getRegion() { return region; }
    public ExecutionStatus getStatus() { return status; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public String getWorkerId() { return workerId; }
    public UUID getEventId() { return eventId; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
}