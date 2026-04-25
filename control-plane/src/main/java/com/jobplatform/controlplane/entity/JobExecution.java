package com.jobplatform.controlplane.entity;

import com.jobplatform.controlplane.enums.ExecutionStatus;
import com.jobplatform.controlplane.enums.Region;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "job_execution",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_job_attempt", columnNames = {"job_id", "attempt_number"}),
                @UniqueConstraint(name = "uq_event_id", columnNames = {"event_id"})
        },
        indexes = {
                @Index(name = "idx_execution_job_id", columnList = "job_id"),
                @Index(name = "idx_execution_region", columnList = "region"),
                @Index(name = "idx_execution_status", columnList = "status"),
                @Index(name = "idx_execution_worker_id", columnList = "worker_id")
        }
)
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_execution_job",
                    foreignKeyDefinition = "FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE"))
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(nullable = true)
    private String workerId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private OffsetDateTime assignedAt;

    @Column(nullable = true)
    private OffsetDateTime startedAt;

    @Column(nullable = true)
    private OffsetDateTime completedAt;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String errorMessage;

    @Version
    private Long version;

    public UUID getId() { return id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(OffsetDateTime assignedAt) { this.assignedAt = assignedAt; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getVersion() { return version; }
}