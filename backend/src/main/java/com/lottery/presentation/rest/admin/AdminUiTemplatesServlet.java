package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.UiTemplateDto;
import com.lottery.application.usecase.system.AdminUiUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminUiTemplatesServlet extends JsonServlet {
    private final AdminUiUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AdminUiTemplatesServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            AdminUiUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length == 0) {
                writeJson(response, 200, new UiTemplateListResponse(useCase.listTemplates(
                        intQuery(request, "limit", 20), intQuery(request, "offset", 0), contextFactory.from(request))));
            } else if (parts.length == 1) {
                writeJson(response, 200, useCase.getTemplate(UUID.fromString(parts[0]), contextFactory.from(request)));
            } else {
                response.sendError(404);
            }
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length != 0) {
                response.sendError(404);
                return;
            }
            UiTemplateRequest body = readJson(request, UiTemplateRequest.class);
            writeJson(response, 201, useCase.createTemplate(body.name(), body.layout(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length != 1) {
                response.sendError(404);
                return;
            }
            UiTemplateRequest body = readJson(request, UiTemplateRequest.class);
            writeJson(response, 200, useCase.updateTemplate(
                    UUID.fromString(parts[0]), body.name(), body.layout(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private String[] path(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.equals("/") ? new String[0] : path.substring(1).split("/");
    }

    private int intQuery(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    public record UiTemplateRequest(String name, Map<String, Object> layout) {
    }

    public record UiTemplateListResponse(List<UiTemplateDto> items) {
    }
}
