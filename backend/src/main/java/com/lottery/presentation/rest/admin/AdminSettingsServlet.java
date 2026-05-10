package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.usecase.system.AdminUiUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public final class AdminSettingsServlet extends JsonServlet {
    private final AdminUiUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AdminSettingsServlet(
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
            if (!isHomePagePath(request)) {
                response.sendError(404);
                return;
            }
            writeJson(response, 200, useCase.getHomePageSettings(contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!isHomePagePath(request)) {
                response.sendError(404);
                return;
            }
            HomePageSettingsRequest body = readJson(request, HomePageSettingsRequest.class);
            writeJson(response, 200, useCase.updateHomePageSettings(
                    body.activeTemplateId(), body.defaultThemeId(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private boolean isHomePagePath(HttpServletRequest request) {
        return "/home-page".equals(request.getPathInfo());
    }

    public record HomePageSettingsRequest(UUID activeTemplateId, UUID defaultThemeId) {
    }
}
