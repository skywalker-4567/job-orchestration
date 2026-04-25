package com.jobplatform.worker.model;

import java.util.Map;
import java.util.UUID;

public class JobExecutionMessage {

    private UUID executionId;
    private UUID jobId;
    private String taskType;
    private Map<String, Object> payload;
    private int attemptNumber;
    private String region;
    private UUID eventId;

    public JobExecutionMessage() {}

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
}