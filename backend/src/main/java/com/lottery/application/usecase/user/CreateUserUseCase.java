package com.lottery.application.usecase.user;

import com.lottery.application.ConflictException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CreateUserCommand;
import com.lottery.application.dto.UserDto;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;

public final class CreateUserUseCase {
    private final UserRepository userRepository;
    private final AuthorizationPort authorizationPort;
    private final PasswordHasher passwordHasher;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final UserMapper mapper;

    public CreateUserUseCase(
            UserRepository userRepository,
            AuthorizationPort authorizationPort,
            PasswordHasher passwordHasher,
            TransactionManager transactionManager,
            DomainClock clock,
            UserMapper mapper) {
        this.userRepository = userRepository;
        this.authorizationPort = authorizationPort;
        this.passwordHasher = passwordHasher;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
    }

    public UserDto execute(CreateUserCommand command, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.USER_CREATE);
        return transactionManager.inTransaction(() -> {
            if (userRepository.existsByEmail(command.email())) {
                throw new ConflictException("USER_EMAIL_ALREADY_EXISTS", "User email already exists");
            }
            if (userRepository.existsByLogin(command.login())) {
                throw new ConflictException("USER_LOGIN_ALREADY_EXISTS", "User login already exists");
            }
            String passwordHash = command.rawPassword() == null || command.rawPassword().isBlank()
                    ? null
                    : passwordHasher.hash(command.rawPassword());
            User user = User.create(command.email(), command.login(), passwordHash, clock.now());
            return mapper.toDto(userRepository.save(user));
        });
    }
}
