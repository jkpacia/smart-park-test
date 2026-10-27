package com.smarkpark.smartparktest.common;

import java.time.Instant;
import java.util.Map;

public class ApiError {
    private final String code, message, path;
    private final Map<String, String> fieldErrors;
    private final Instant timestamp;

    public ApiError(String code, String message, Map<String, String> fieldErrors, Instant timestamp, String path) {
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = timestamp;
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}

