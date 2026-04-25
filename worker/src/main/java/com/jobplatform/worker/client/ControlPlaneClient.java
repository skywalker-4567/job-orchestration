package com.jobplatform.worker.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class ControlPlaneClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ControlPlaneClient(
            RestTemplate restTemplate,
            @Value("${control-plane.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void startExecution(UUID executionId, String workerId) {
        String url = baseUrl + "/executions/" + executionId + "/start";
        restTemplate.postForEntity(url, Map.of("workerId", workerId), Void.class);
    }

    public void completeExecution(UUID executionId) {
        String url = baseUrl + "/executions/" + executionId + "/complete";
        restTemplate.postForEntity(url, Map.of(), Void.class);
    }

    public void failExecution(UUID executionId, String errorMessage) {
        String url = baseUrl + "/executions/" + executionId + "/fail";
        restTemplate.postForEntity(url, Map.of("errorMessage", errorMessage), Void.class);
    }
}