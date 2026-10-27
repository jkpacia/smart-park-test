package com.smarkpark.smartparktest.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> business(ApiException e, HttpServletRequest r) {
        return error(e.getStatus(), e.getCode(), e.getMessage(), Map.of(), r);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fields, r);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformed(HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Malformed JSON or invalid field type", Map.of(), r);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(HttpServletRequest r) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid username or password", Map.of(), r);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> duplicate(HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "A resource with this identifier already exists", Map.of(), r);
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> concurrency(HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_CHANGE", "Concurrent update; retry the request", Map.of(), r);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> missing(HttpServletRequest r) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Endpoint was not found", Map.of(), r);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(HttpServletRequest r) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method is not supported", Map.of(), r);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> media(HttpServletRequest r) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Use application/json", Map.of(), r);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, Map<String, String> fields, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(code, message, fields, clock.instant(), request.getRequestURI()));
    }
}

