package com.lottery.application.usecase.auth;

import com.lottery.application.ConflictException;
import com.lottery.application.command.RegisterUserCommand;
import com.lottery.application.dto.UserDto;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.RoleCodes;

public final class RegisterUserUseCase {
    private static final String OWNER_LOGIN = "owner";

    private final UserRepository userRepository;
    private final RbacRepository rbacRepository;
    private final PasswordHasher passwordHasher;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final UserMapper mapper;

    public RegisterUserUseCase(
            UserRepository userRepository,
            RbacRepository rbacRepository,
            PasswordHasher passwordHasher,
            TransactionManager transactionManager,
            DomainClock clock,
            UserMapper mapper) {
        this.userRepository = userRepository;
        this.rbacRepository = rbacRepository;
        this.passwordHasher = passwordHasher;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
    }

    public UserDto execute(RegisterUserCommand command) {
        return transactionManager.inTransaction(() -> {
            String login = normalizeLogin(command);
            boolean firstUser = !userRepository.existsAny();
            if (userRepository.existsByEmail(command.email())) {
                throw new ConflictException("USER_EMAIL_ALREADY_EXISTS", "User email already exists");
            }
            if (userRepository.existsByLogin(login)) {
                throw new ConflictException("USER_LOGIN_ALREADY_EXISTS", "User login already exists");
            }
            User user = User.create(command.email(), login, passwordHasher.hash(command.rawPassword()), clock.now());
            User saved = userRepository.save(user);
            rbacRepository.assignRoleByCode(saved.id(), initialRoleCode(firstUser, login));
            return mapper.toDto(saved);
        });
    }

    private String normalizeLogin(RegisterUserCommand command) {
        return command.login() == null ? command.email().trim() : command.login().trim();
    }

    private String initialRoleCode(boolean firstUser, String login) {
        return firstUser && OWNER_LOGIN.equals(login) ? RoleCodes.ADMIN : RoleCodes.CLIENT;
    }
}
