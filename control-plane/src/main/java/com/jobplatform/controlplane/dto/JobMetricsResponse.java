package com.jobplatform.controlplane.dto;

public class JobMetricsResponse {

    private long totalJobs;
    private long completedJobs;
    private long failedJobs;
    private long inProgressJobs;

    public JobMetricsResponse(long totalJobs, long completedJobs, long failedJobs, long inProgressJobs) {
        this.totalJobs = totalJobs;
        this.completedJobs = completedJobs;
        this.failedJobs = failedJobs;
        this.inProgressJobs = inProgressJobs;
    }

    public long getTotalJobs() { return totalJobs; }
    public long getCompletedJobs() { return completedJobs; }
    public long getFailedJobs() { return failedJobs; }
    public long getInProgressJobs() { return inProgressJobs; }
}