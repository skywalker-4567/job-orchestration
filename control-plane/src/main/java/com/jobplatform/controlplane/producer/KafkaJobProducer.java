package com.jobplatform.controlplane.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class KafkaJobProducer implements JobProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaJobProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishJobExecution(
            UUID executionId,
            UUID jobId,
            String taskType,
            Map<String, Object> payload,
            int attemptNumber,
            String region,
            UUID eventId
    ) {
        JobExecutionMessage message = new JobExecutionMessage(
                executionId,
                jobId,
                taskType,
                payload,
                attemptNumber,
                region,
                eventId
        );

        String topic = "jobs." + region;
        kafkaTemplate.send(topic, executionId.toString(), message);
    }
}