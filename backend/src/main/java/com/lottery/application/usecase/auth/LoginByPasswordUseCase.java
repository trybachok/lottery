package com.lottery.application.usecase.auth;

import com.lottery.application.UnauthorizedException;
import com.lottery.application.command.LoginByPasswordCommand;
import com.lottery.application.dto.AuthResponseDto;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.port.auth.TokenIssuerPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.valueobject.UserStatus;
import java.util.Set;

public final class LoginByPasswordUseCase {
    private final UserRepository userRepository;
    private final RbacRepository rbacRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuerPort tokenIssuerPort;
    private final TransactionManager transactionManager;
    private final UserMapper mapper;

    public LoginByPasswordUseCase(
            UserRepository userRepository,
            RbacRepository rbacRepository,
            PasswordHasher passwordHasher,
            TokenIssuerPort tokenIssuerPort,
            TransactionManager transactionManager,
            UserMapper mapper) {
        this.userRepository = userRepository;
        this.rbacRepository = rbacRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuerPort = tokenIssuerPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public AuthResponseDto execute(LoginByPasswordCommand command) {
        return transactionManager.inTransaction(() -> {
            User user = userRepository
                    .findByEmailOrLogin(command.loginOrEmail())
                    .orElseThrow(() -> new UnauthorizedException("Invalid login or password"));
            if (user.status() != UserStatus.ACTIVE) {
                throw new UnauthorizedException("User is not active");
            }
            String passwordHash = user.passwordHash().orElseThrow(() -> new UnauthorizedException("Invalid login or password"));
            if (!passwordHasher.verify(command.rawPassword(), passwordHash)) {
                throw new UnauthorizedException("Invalid login or password");
            }
            TokenIssuerPort.IssuedToken token = tokenIssuerPort.issue(user.id());
            Set<String> roleCodes = rbacRepository.findRoleCodesByUserId(user.id());
            Set<String> permissions = rbacRepository.findPermissionCodesByUserId(user.id());
            return new AuthResponseDto(
                    token.value(),
                    "Bearer",
                    token.expiresAt(),
                    mapper.toDto(user),
                    roleCodes,
                    permissions);
        });
    }
}
