package com.lottery.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.usecase.system.GetOpenApiDocumentUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class OpenApiServlet extends JsonServlet {
    private final GetOpenApiDocumentUseCase getOpenApiDocumentUseCase;
    private final ServletUseCaseContextFactory contextFactory;

    public OpenApiServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            GetOpenApiDocumentUseCase getOpenApiDocumentUseCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.getOpenApiDocumentUseCase = getOpenApiDocumentUseCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String document = getOpenApiDocumentUseCase.execute(() -> contextFactory.from(request));
            response.setStatus(200);
            response.setContentType("application/yaml;charset=UTF-8");
            response.getWriter().write(document);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }
}
