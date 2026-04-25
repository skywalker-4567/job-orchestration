package com.jobplatform.worker.service;

import com.jobplatform.worker.client.ControlPlaneClient;
import com.jobplatform.worker.model.JobExecutionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class WorkerExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkerExecutionService.class);

    private final ControlPlaneClient controlPlaneClient;
    private final IdempotencyService idempotencyService;
    private final Random random = new Random();
    private final String workerId;

    public WorkerExecutionService(
            ControlPlaneClient controlPlaneClient,
            IdempotencyService idempotencyService,
            @Value("${worker.region}") String region
    ) {
        this.controlPlaneClient = controlPlaneClient;
        this.idempotencyService = idempotencyService;
        this.workerId = "worker-" + region + "-" + UUID.randomUUID();
    }

    public void process(JobExecutionMessage message) {
        if (idempotencyService.isProcessed(message.getEventId())) {
            return;
        }

        log.info("Received execution: executionId={}, region={}",
                message.getExecutionId(), message.getRegion());

        try {
            controlPlaneClient.startExecution(message.getExecutionId(), workerId);
        } catch (Exception e) {
            log.error("Failed to start execution: executionId={}, error={}",
                    message.getExecutionId(), e.getMessage());
            return;
        }

        try {
            int sleepMs = 1000 + random.nextInt(2000);
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = random.nextDouble() < 0.80;

        if (success) {
            try {
                controlPlaneClient.completeExecution(message.getExecutionId());
                log.info("Execution success: executionId={}", message.getExecutionId());
            } catch (Exception e) {
                log.error("Failed to complete execution: executionId={}, error={}",
                        message.getExecutionId(), e.getMessage());
                return;
            }
        } else {
            try {
                controlPlaneClient.failExecution(message.getExecutionId(), "Simulated failure");
                log.info("Execution failed: executionId={}, error=Simulated failure",
                        message.getExecutionId());
            } catch (Exception e) {
                log.error("Failed to report execution failure: executionId={}, error={}",
                        message.getExecutionId(), e.getMessage());
                return;
            }
        }

        idempotencyService.markProcessed(message.getEventId());
    }
}