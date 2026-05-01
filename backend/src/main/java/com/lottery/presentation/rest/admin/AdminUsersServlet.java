package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.RoleDto;
import com.lottery.application.dto.UserDto;
import com.lottery.application.usecase.admin.AdminRbacUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class AdminUsersServlet extends JsonServlet {
    private final AdminRbacUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AdminUsersServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            AdminRbacUseCase useCase,
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
                writeJson(response, 200, new UserListResponse(useCase.listUsers(
                        intQuery(request, "limit", 20), intQuery(request, "offset", 0), contextFactory.from(request))));
            } else if (parts.length == 1) {
                writeJson(response, 200, useCase.getUser(UUID.fromString(parts[0]), contextFactory.from(request)));
            } else if (parts.length == 2 && "roles".equals(parts[1])) {
                writeJson(response, 200, new RoleListResponse(useCase.listUserRoles(UUID.fromString(parts[0]), contextFactory.from(request))));
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
                UserRequest body = readJson(request, UserRequest.class);
                writeJson(response, 201, useCase.createUser(body.email(), body.login(), body.password(), contextFactory.from(request)));
            } else if (parts.length == 2 && "roles".equals(parts[1])) {
                RoleAssignmentRequest body = readJson(request, RoleAssignmentRequest.class);
                useCase.assignUserRole(UUID.fromString(parts[0]), body.roleId(), contextFactory.from(request));
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
        update(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length == 1) {
                useCase.deleteUser(UUID.fromString(parts[0]), contextFactory.from(request));
                response.setStatus(204);
            } else if (parts.length == 3 && "roles".equals(parts[1])) {
                useCase.removeUserRole(UUID.fromString(parts[0]), UUID.fromString(parts[2]), contextFactory.from(request));
                response.setStatus(204);
            } else {
                response.sendError(404);
            }
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private void update(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length != 1) {
                response.sendError(404);
                return;
            }
            UserRequest body = readJson(request, UserRequest.class);
            writeJson(response, 200, useCase.updateUser(
                    UUID.fromString(parts[0]), body.email(), body.login(), body.password(), body.status(), contextFactory.from(request)));
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

    public record UserRequest(String email, String login, String password, String status) {
    }

    public record RoleAssignmentRequest(UUID roleId) {
    }

    public record UserListResponse(List<UserDto> items) {
    }

    public record RoleListResponse(List<RoleDto> items) {
    }
}
