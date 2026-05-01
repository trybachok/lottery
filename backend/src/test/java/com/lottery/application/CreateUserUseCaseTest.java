package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.command.CreateUserCommand;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.user.CreateUserUseCase;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CreateUserUseCaseTest {
    @Test
    void hashesPasswordAndReturnsDtoWithoutPasswordHash() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        CreateUserUseCase useCase = new CreateUserUseCase(
                repository,
                requirePermission(),
                testPasswordHasher(),
                directTransaction(),
                fixedClock(),
                new UserMapper());

        var result = useCase.execute(
                new CreateUserCommand("client@example.com", "client", "secret"),
                new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.USER_CREATE), "req_test"));

        assertEquals("client@example.com", result.email());
        assertEquals("hash:secret", repository.saved.passwordHash().orElseThrow());
    }

    @Test
    void rejectsDuplicateEmail() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.emailExists = true;
        CreateUserUseCase useCase = new CreateUserUseCase(
                repository,
                requirePermission(),
                testPasswordHasher(),
                directTransaction(),
                fixedClock(),
                new UserMapper());

        assertThrows(
                ConflictException.class,
                () -> useCase.execute(
                        new CreateUserCommand("client@example.com", "client", "secret"),
                        new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.USER_CREATE), "req_test")));
    }

    private static AuthorizationPort requirePermission() {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
                if (!context.permissions().contains(permissionCode)) {
                    throw new ForbiddenException(permissionCode);
                }
            }

            @Override
            public boolean hasRole(UseCaseContext context, String roleCode) {
                return false;
            }
        };
    }

    private static PasswordHasher testPasswordHasher() {
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
        private boolean emailExists;
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
            return emailExists;
        }

        @Override
        public boolean existsByLogin(String login) {
            return false;
        }
    }
}
