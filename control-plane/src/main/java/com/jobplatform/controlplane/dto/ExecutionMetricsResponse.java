package com.jobplatform.controlplane.dto;

public class ExecutionMetricsResponse {

    private long totalExecutions;
    private long runningExecutions;
    private long completedExecutions;
    private long failedExecutions;
    private long timeoutExecutions;
    private double avgExecutionTimeMs;

    public ExecutionMetricsResponse(
            long totalExecutions,
            long runningExecutions,
            long completedExecutions,
            long failedExecutions,
            long timeoutExecutions,
            double avgExecutionTimeMs
    ) {
        this.totalExecutions = totalExecutions;
        this.runningExecutions = runningExecutions;
        this.completedExecutions = completedExecutions;
        this.failedExecutions = failedExecutions;
        this.timeoutExecutions = timeoutExecutions;
        this.avgExecutionTimeMs = avgExecutionTimeMs;
    }

    public long getTotalExecutions() { return totalExecutions; }
    public long getRunningExecutions() { return runningExecutions; }
    public long getCompletedExecutions() { return completedExecutions; }
    public long getFailedExecutions() { return failedExecutions; }
    public long getTimeoutExecutions() { return timeoutExecutions; }
    public double getAvgExecutionTimeMs() { return avgExecutionTimeMs; }
}