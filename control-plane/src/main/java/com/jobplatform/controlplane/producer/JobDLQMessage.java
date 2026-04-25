package com.jobplatform.controlplane.producer;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class JobDLQMessage {

    private UUID jobId;
    private UUID executionId;
    private String taskType;
    private Map<String, Object> payload;
    private int attemptNumber;
    private String region;
    private String errorMessage;
    private UUID eventId;
    private OffsetDateTime failedAt;

    public JobDLQMessage() {}

    public JobDLQMessage(
            UUID jobId,
            UUID executionId,
            String taskType,
            Map<String, Object> payload,
            int attemptNumber,
            String region,
            String errorMessage,
            UUID eventId,
            OffsetDateTime failedAt
    ) {
        this.jobId = jobId;
        this.executionId = executionId;
        this.taskType = taskType;
        this.payload = payload;
        this.attemptNumber = attemptNumber;
        this.region = region;
        this.errorMessage = errorMessage;
        this.eventId = eventId;
        this.failedAt = failedAt;
    }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public OffsetDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(OffsetDateTime failedAt) { this.failedAt = failedAt; }
}