package com.jobplatform.controlplane.controller;

import com.jobplatform.controlplane.dto.FailExecutionRequest;
import com.jobplatform.controlplane.dto.StartExecutionRequest;
import com.jobplatform.controlplane.service.ExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startExecution(
            @PathVariable UUID id,
            @RequestBody StartExecutionRequest request
    ) {
        executionService.startExecution(id, request.getWorkerId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeExecution(@PathVariable UUID id) {
        executionService.completeExecution(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<Void> failExecution(
            @PathVariable UUID id,
            @RequestBody FailExecutionRequest request
    ) {
        executionService.failExecution(id, request.getErrorMessage());
        return ResponseEntity.ok().build();
    }
}