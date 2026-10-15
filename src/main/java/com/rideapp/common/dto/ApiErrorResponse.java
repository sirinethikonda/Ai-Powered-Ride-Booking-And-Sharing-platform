package com.rideapp.common.dto;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {

    private boolean success = false;
    private String errorCode;
    private String message;
    private List<String> details;
    private String path;
    private Instant timestamp;

    public ApiErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(String errorCode, String message, List<String> details, String path) {
        this.success = false;
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.path = path;
        this.timestamp = Instant.now();
    }

    public static ApiErrorResponse of(String errorCode, String message, String path) {
        return new ApiErrorResponse(errorCode, message, List.of(), path);
    }

    public static ApiErrorResponse of(String errorCode, String message, List<String> details, String path) {
        return new ApiErrorResponse(errorCode, message, details, path);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
