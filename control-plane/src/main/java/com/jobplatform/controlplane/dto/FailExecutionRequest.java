package com.jobplatform.controlplane.dto;

public class FailExecutionRequest {
    private String errorMessage;

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}