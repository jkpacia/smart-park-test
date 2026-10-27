package com.smarkpark.smartparktest.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;

@Component
public class JsonErrors {
    private final ObjectMapper json;
    private final Clock clock;

    public JsonErrors(ObjectMapper json, Clock clock) {
        this.json = json;
        this.clock = clock;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), new ApiError(code, message, Map.of(), clock.instant(), request.getRequestURI()));
    }
}

