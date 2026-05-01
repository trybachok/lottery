package com.lottery.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.presentation.error.GlobalErrorHandler;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class JsonServlet extends HttpServlet {
    protected final ObjectMapper objectMapper;
    private final GlobalErrorHandler errorHandler;

    protected JsonServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler) {
        this.objectMapper = objectMapper;
        this.errorHandler = errorHandler;
    }

    protected <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        return objectMapper.readValue(request.getInputStream(), type);
    }

    protected void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    protected void handleException(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        errorHandler.handle(request, response, exception);
    }
}
