package com.lottery.presentation.rest.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.RunDrawResultDto;
import com.lottery.application.usecase.draw.RunDrawUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public final class RunDrawServlet extends JsonServlet {
    private final RunDrawUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public RunDrawServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            RunDrawUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UUID drawId = drawIdFromPath(request);
            RunDrawResultDto result = useCase.execute(drawId, contextFactory.from(request));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private UUID drawIdFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.endsWith("/run")) {
            throw new IllegalArgumentException("Expected path /{drawId}/run");
        }
        String drawId = pathInfo.substring(1, pathInfo.length() - "/run".length());
        return UUID.fromString(drawId);
    }
}
