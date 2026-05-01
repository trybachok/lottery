package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.command.LoginByPasswordCommand;
import com.lottery.application.command.RegisterUserCommand;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.port.auth.TokenIssuerPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.auth.LoginByPasswordUseCase;
import com.lottery.application.usecase.auth.RegisterUserUseCase;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.RoleCodes;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AuthUseCaseTest {
    @Test
    void registerCreatesClientWithHashedPassword() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryRbacRepository rbac = new InMemoryRbacRepository();
        RegisterUserUseCase useCase = new RegisterUserUseCase(
                users,
                rbac,
                passwordHasher(),
                directTransaction(),
                fixedClock(),
                new UserMapper());

        var result = useCase.execute(new RegisterUserCommand("client@example.com", null, "secret"));

        assertEquals("client@example.com", result.email());
        assertEquals("hash:secret", users.saved.passwordHash().orElseThrow());
        assertEquals(Set.of(RoleCodes.CLIENT), rbac.roles.get(users.saved.id()));
    }

    @Test
    void loginIssuesOpaqueBearerToken() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        User user = User.create("client@example.com", "client", "hash:secret", Instant.parse("2026-05-01T00:00:00Z"));
        users.save(user);
        LoginByPasswordUseCase useCase = new LoginByPasswordUseCase(
                users,
                passwordHasher(),
                userId -> new TokenIssuerPort.IssuedToken("token-" + userId, Instant.parse("2026-05-01T00:15:00Z")),
                directTransaction(),
                new UserMapper());

        var result = useCase.execute(new LoginByPasswordCommand("client", "secret"));

        assertEquals("Bearer", result.tokenType());
        assertEquals("token-" + user.id(), result.accessToken());
    }

    @Test
    void loginRejectsWrongPassword() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        users.save(User.create("client@example.com", "client", "hash:secret", Instant.parse("2026-05-01T00:00:00Z")));
        LoginByPasswordUseCase useCase = new LoginByPasswordUseCase(
                users,
                passwordHasher(),
                userId -> new TokenIssuerPort.IssuedToken("token", Instant.parse("2026-05-01T00:15:00Z")),
                directTransaction(),
                new UserMapper());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(new LoginByPasswordCommand("client", "bad")));
    }

    private static PasswordHasher passwordHasher() {
        return new PasswordHasher() {
            @Override
            public String hash(String rawPassword) {
                return "hash:" + rawPassword;
            }

            @Override
            public boolean verify(String rawPassword, String passwordHash) {
                return ("hash:" + rawPassword).equals(passwordHash);
            }
        };
    }

    private static TransactionManager directTransaction() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(TransactionalWork<T> work) {
                return work.execute();
            }
        };
    }

    private static DomainClock fixedClock() {
        return () -> Instant.parse("2026-05-01T00:00:00Z");
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();
        private User saved;

        @Override
        public User save(User user) {
            saved = user;
            users.put(user.id(), user);
            return user;
        }

        @Override
        public User update(User user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmailOrLogin(String loginOrEmail) {
            return users.values().stream()
                    .filter(user -> user.email().equals(loginOrEmail) || user.login().equals(loginOrEmail))
                    .findFirst();
        }

        @Override
        public List<User> findAll(int limit, int offset) {
            return users.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(user -> user.email().equals(email));
        }

        @Override
        public boolean existsByLogin(String login) {
            return users.values().stream().anyMatch(user -> user.login().equals(login));
        }
    }

    private static final class InMemoryRbacRepository implements RbacRepository {
        private final Map<UUID, Set<String>> roles = new HashMap<>();

        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            return Set.of();
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            return roles.getOrDefault(userId, Set.of());
        }

        @Override
        public void assignRoleByCode(UUID userId, String roleCode) {
            roles.computeIfAbsent(userId, ignored -> new HashSet<>()).add(roleCode);
        }
    }
}
