package com.lottery.presentation.rest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.CreateUserCommand;
import com.lottery.application.dto.UserDto;
import com.lottery.application.usecase.user.CreateUserUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class CreateUserServlet extends JsonServlet {
    private final CreateUserUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public CreateUserServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            CreateUserUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            CreateUserRequest body = readJson(request, CreateUserRequest.class);
            UserDto result = useCase.execute(
                    new CreateUserCommand(body.email(), body.login(), body.password()),
                    contextFactory.from(request));
            writeJson(response, 201, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    public record CreateUserRequest(String email, String login, String password) {
    }
}
