package com.jobplatform.controlplane.producer;

import java.util.Map;
import java.util.UUID;

public interface JobProducer {

    void publishJobExecution(
            UUID executionId,
            UUID jobId,
            String taskType,
            Map<String, Object> payload,
            int attemptNumber,
            String region,
            UUID eventId
    );
}