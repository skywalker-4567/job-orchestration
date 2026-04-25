package com.jobplatform.worker.consumer;

import com.jobplatform.worker.model.JobExecutionMessage;
import com.jobplatform.worker.service.WorkerExecutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobExecutionConsumer {

    private final WorkerExecutionService workerExecutionService;
    private final String region;

    public JobExecutionConsumer(
            WorkerExecutionService workerExecutionService,
            @Value("${worker.region}") String region
    ) {
        this.workerExecutionService = workerExecutionService;
        this.region = region;
    }

    @KafkaListener(topics = "jobs.${worker.region}")
    public void consume(JobExecutionMessage message) {
        if (!region.equals(message.getRegion())) {
            return;
        }
        try {
            workerExecutionService.process(message);
        } catch (Exception e) {
            // swallow — prevent consumer crash loop. Proper DLQ in Phase 3
        }
    }
}