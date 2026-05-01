package com.lottery.presentation.rest.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.LoginByPasswordCommand;
import com.lottery.application.dto.AuthResponseDto;
import com.lottery.application.usecase.auth.LoginByPasswordUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class LoginServlet extends JsonServlet {
    private final LoginByPasswordUseCase useCase;

    public LoginServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler, LoginByPasswordUseCase useCase) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LoginRequest body = readJson(request, LoginRequest.class);
            AuthResponseDto result = useCase.execute(new LoginByPasswordCommand(body.loginOrEmail(), body.password()));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    public record LoginRequest(String loginOrEmail, String password) {
    }
}
