package com.lottery.presentation.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.TokenVerifierPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Permission;
import com.lottery.domain.model.Role;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import com.lottery.presentation.middleware.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ServletUseCaseContextFactoryTest {
    @Test
    void loadsRbacCodesInsideTransaction() {
        UUID userId = UUID.randomUUID();
        TrackingTransactionManager transactionManager = new TrackingTransactionManager();
        ServletUseCaseContextFactory factory = new ServletUseCaseContextFactory(
                token -> new TokenVerifierPort.AuthenticatedPrincipal(userId, Set.of()),
                new TransactionAwareRbac(transactionManager),
                transactionManager);

        UseCaseContext context = factory.from(request());

        assertTrue(transactionManager.used);
        assertEquals(userId, context.actorUserId());
        assertEquals(Set.of(PermissionCodes.TICKET_READ), context.permissions());
        assertEquals(Set.of(RoleCodes.ADMIN), context.actorRoleCodes());
        assertEquals("req_test", context.requestId());
    }

    private static HttpServletRequest request() {
        return (HttpServletRequest) Proxy.newProxyInstance(
                ServletUseCaseContextFactoryTest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getHeader" -> "Authorization".equals(args[0]) ? "Bearer token" : null;
                    case "getRemoteAddr" -> "127.0.0.1";
                    case "getAttribute" -> RequestContext.ATTRIBUTE_NAME.equals(args[0])
                            ? new RequestContext("req_test", null, 1L)
                            : null;
                    default -> null;
                });
    }

    private static final class TrackingTransactionManager implements TransactionManager {
        private boolean active;
        private boolean used;

        @Override
        public <T> T inTransaction(TransactionalWork<T> work) {
            used = true;
            active = true;
            try {
                return work.execute();
            } finally {
                active = false;
            }
        }
    }

    private static final class TransactionAwareRbac implements RbacRepository {
        private final TrackingTransactionManager transactionManager;

        private TransactionAwareRbac(TrackingTransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            assertTrue(transactionManager.active);
            return Set.of(PermissionCodes.TICKET_READ);
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            assertTrue(transactionManager.active);
            return Set.of(RoleCodes.ADMIN);
        }

        @Override
        public void assignRoleByCode(UUID userId, String roleCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Role> findAllRoles(int limit, int offset) {
            return List.of();
        }

        @Override
        public Optional<Role> findRoleById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Permission> findAllPermissions(int limit, int offset) {
            return List.of();
        }
    }
}
