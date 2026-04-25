package com.jobplatform.controlplane.dto;

import com.jobplatform.controlplane.enums.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class JobRequest {

    @NotBlank(message = "taskType is required")
    private String taskType;

    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    @NotNull(message = "priority is required")
    private Priority priority;

    @Min(value = 1, message = "maxAttempts must be at least 1")
    private Integer maxAttempts;

    @Min(value = 1, message = "timeoutSeconds must be at least 1")
    private Integer timeoutSeconds;

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}