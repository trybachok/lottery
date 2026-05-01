package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.PermissionDto;
import com.lottery.application.dto.RoleDto;
import com.lottery.application.usecase.admin.AdminRbacUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class AdminRolesServlet extends JsonServlet {
    private final AdminRbacUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AdminRolesServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler, AdminRbacUseCase useCase,
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
                writeJson(response, 200, new RoleListResponse(useCase.listRoles(
                        intQuery(request, "limit", 20), intQuery(request, "offset", 0), contextFactory.from(request))));
            } else if (parts.length == 1) {
                writeJson(response, 200, useCase.getRole(UUID.fromString(parts[0]), contextFactory.from(request)));
            } else if (parts.length == 2 && "permissions".equals(parts[1])) {
                writeJson(response, 200, new PermissionListResponse(
                        useCase.listRolePermissions(UUID.fromString(parts[0]), contextFactory.from(request))));
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
                RoleRequest body = readJson(request, RoleRequest.class);
                writeJson(response, 201, useCase.createRole(body.code(), body.name(), body.description(), contextFactory.from(request)));
            } else if (parts.length == 2 && "permissions".equals(parts[1])) {
                PermissionAssignmentRequest body = readJson(request, PermissionAssignmentRequest.class);
                useCase.assignRolePermission(UUID.fromString(parts[0]), body.permissionId(), contextFactory.from(request));
                response.setStatus(204);
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
            RoleRequest body = readJson(request, RoleRequest.class);
            writeJson(response, 200, useCase.updateRole(
                    UUID.fromString(parts[0]), body.code(), body.name(), body.description(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length == 1) {
                useCase.deleteRole(UUID.fromString(parts[0]), contextFactory.from(request));
                response.setStatus(204);
            } else if (parts.length == 3 && "permissions".equals(parts[1])) {
                useCase.removeRolePermission(UUID.fromString(parts[0]), UUID.fromString(parts[2]), contextFactory.from(request));
                response.setStatus(204);
            } else {
                response.sendError(404);
            }
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

    public record RoleRequest(String code, String name, String description) {
    }

    public record PermissionAssignmentRequest(UUID permissionId) {
    }

    public record RoleListResponse(List<RoleDto> items) {
    }

    public record PermissionListResponse(List<PermissionDto> items) {
    }
}
