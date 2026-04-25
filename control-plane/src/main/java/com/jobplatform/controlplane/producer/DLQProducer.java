package com.jobplatform.controlplane.producer;

public interface DLQProducer {
    void publishDLQ(JobDLQMessage message);
}