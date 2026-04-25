package com.jobplatform.controlplane.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaDLQProducer implements DLQProducer {

    private static final String DLQ_TOPIC = "jobs.DLQ";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaDLQProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishDLQ(JobDLQMessage message) {
        kafkaTemplate.send(DLQ_TOPIC, message.getJobId().toString(), message);
    }
}