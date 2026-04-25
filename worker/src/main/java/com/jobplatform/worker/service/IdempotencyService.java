package com.jobplatform.worker.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final Set<UUID> processedEvents = ConcurrentHashMap.newKeySet();

    public boolean isProcessed(UUID eventId) {
        return processedEvents.contains(eventId);
    }

    public void markProcessed(UUID eventId) {
        processedEvents.add(eventId);
    }
}