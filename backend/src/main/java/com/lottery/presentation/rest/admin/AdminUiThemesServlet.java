package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.UiThemeDto;
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

public final class AdminUiThemesServlet extends JsonServlet {
    private final AdminUiUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AdminUiThemesServlet(
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
                writeJson(response, 200, new UiThemeListResponse(useCase.listThemes(
                        intQuery(request, "limit", 20), intQuery(request, "offset", 0), contextFactory.from(request))));
            } else if (parts.length == 1) {
                writeJson(response, 200, useCase.getTheme(UUID.fromString(parts[0]), contextFactory.from(request)));
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
            if (parts.length == 0) {
                UiThemeRequest body = readJson(request, UiThemeRequest.class);
                writeJson(response, 201, useCase.createTheme(
                        body.name(), body.tokens(), Boolean.TRUE.equals(body.defaultTheme()), contextFactory.from(request)));
            } else if (parts.length == 2 && "default".equals(parts[1])) {
                writeJson(response, 200, useCase.setDefaultTheme(UUID.fromString(parts[0]), contextFactory.from(request)));
            } else {
                response.sendError(404);
            }
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
            UiThemeRequest body = readJson(request, UiThemeRequest.class);
            writeJson(response, 200, useCase.updateTheme(
                    UUID.fromString(parts[0]),
                    body.name(),
                    body.tokens(),
                    Boolean.TRUE.equals(body.defaultTheme()),
                    contextFactory.from(request)));
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

    public record UiThemeRequest(String name, Map<String, Object> tokens, Boolean defaultTheme) {
    }

    public record UiThemeListResponse(List<UiThemeDto> items) {
    }
}
