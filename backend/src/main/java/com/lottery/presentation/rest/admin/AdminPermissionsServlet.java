package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.PermissionDto;
import com.lottery.application.usecase.admin.AdminRbacUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class AdminPermissionsServlet extends JsonServlet {
    private final AdminRbacUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AdminPermissionsServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler, AdminRbacUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = request.getPathInfo();
            if (path == null || path.equals("/")) {
                writeJson(response, 200, new PermissionListResponse(useCase.listPermissions(
                        intQuery(request, "limit", 20), intQuery(request, "offset", 0), contextFactory.from(request))));
            } else {
                writeJson(response, 200, useCase.getPermission(id(request), contextFactory.from(request)));
            }
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PermissionRequest body = readJson(request, PermissionRequest.class);
            writeJson(response, 201, useCase.createPermission(body.code(), body.description(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UUID id = id(request);
            PermissionRequest body = readJson(request, PermissionRequest.class);
            writeJson(response, 200, useCase.updatePermission(id, body.code(), body.description(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            useCase.deletePermission(id(request), contextFactory.from(request));
            response.setStatus(204);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private UUID id(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            throw new IllegalArgumentException("id is required");
        }
        return UUID.fromString(path.substring(1));
    }

    private int intQuery(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    public record PermissionRequest(String code, String description) {
    }

    public record PermissionListResponse(List<PermissionDto> items) {
    }
}
