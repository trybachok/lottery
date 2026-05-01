package com.lottery.presentation.rest.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public final class HealthServlet extends JsonServlet {
    public HealthServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler) {
        super(objectMapper, errorHandler);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJson(response, 200, Map.of("status", "UP", "checkedAt", Instant.now().toString()));
    }
}
