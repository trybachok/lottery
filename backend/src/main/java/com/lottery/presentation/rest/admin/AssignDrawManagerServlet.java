package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.usecase.draw.AssignDrawManagerUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public final class AssignDrawManagerServlet extends JsonServlet {
    private final AssignDrawManagerUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AssignDrawManagerServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler,
            AssignDrawManagerUseCase useCase, ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = request.getPathInfo() == null ? new String[0] : request.getPathInfo().substring(1).split("/");
            if (parts.length != 2 || !"assign-manager".equals(parts[1])) {
                response.sendError(404);
                return;
            }
            AssignManagerRequest body = readJson(request, AssignManagerRequest.class);
            writeJson(response, 200, useCase.execute(UUID.fromString(parts[0]), body.managerId(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    public record AssignManagerRequest(UUID managerId) {
    }
}
