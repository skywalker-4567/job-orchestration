package com.jobplatform.controlplane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobplatform.controlplane.entity.OutboxEvent;
import com.jobplatform.controlplane.producer.JobExecutionMessage;
import com.jobplatform.controlplane.producer.JobProducer;
import com.jobplatform.controlplane.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final JobProducer jobProducer;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            JobProducer jobProducer,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.jobProducer = jobProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 3000)
    public void publishOutboxEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();

        for (OutboxEvent event : unpublished) {
            try {
                JobExecutionMessage message = objectMapper.readValue(
                        event.getPayload(),
                        JobExecutionMessage.class
                );

                jobProducer.publishJobExecution(
                        message.getExecutionId(),
                        message.getJobId(),
                        message.getTaskType(),
                        message.getPayload(),
                        message.getAttemptNumber(),
                        message.getRegion(),
                        message.getEventId()
                );

                event.setPublished(true);
                outboxEventRepository.save(event);

                log.info("Outbox event published: eventId={}, executionId={}",
                        event.getId(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish outbox event: eventId={}, error={}",
                        event.getId(), e.getMessage());
            }
        }
    }
}