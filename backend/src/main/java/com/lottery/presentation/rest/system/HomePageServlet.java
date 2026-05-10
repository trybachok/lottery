package com.lottery.presentation.rest.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.usecase.system.GetHomePageUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class HomePageServlet extends JsonServlet {
    private final GetHomePageUseCase useCase;

    public HomePageServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler, GetHomePageUseCase useCase) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeJson(response, 200, useCase.get());
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }
}
