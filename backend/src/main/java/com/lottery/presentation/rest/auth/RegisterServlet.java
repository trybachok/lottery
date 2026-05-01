package com.lottery.presentation.rest.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.RegisterUserCommand;
import com.lottery.application.dto.UserDto;
import com.lottery.application.usecase.auth.RegisterUserUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class RegisterServlet extends JsonServlet {
    private final RegisterUserUseCase useCase;

    public RegisterServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler, RegisterUserUseCase useCase) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            RegisterRequest body = readJson(request, RegisterRequest.class);
            UserDto result = useCase.execute(new RegisterUserCommand(body.email(), body.login(), body.password()));
            writeJson(response, 201, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    public record RegisterRequest(String email, String login, String password) {
    }
}
