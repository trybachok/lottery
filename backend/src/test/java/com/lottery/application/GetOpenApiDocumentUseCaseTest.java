package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.system.GetOpenApiDocumentUseCase;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class GetOpenApiDocumentUseCaseTest {
    @Test
    void returnsDocumentForAdmin() {
        GetOpenApiDocumentUseCase useCase = new GetOpenApiDocumentUseCase(() -> "openapi: 3.1.0", auth(), tx());

        String document = useCase.execute(() -> context(Set.of(RoleCodes.ADMIN)));

        assertEquals("openapi: 3.1.0", document);
    }

    @Test
    void rejectsNonAdmin() {
        GetOpenApiDocumentUseCase useCase = new GetOpenApiDocumentUseCase(() -> "openapi: 3.1.0", auth(), tx());

        assertThrows(ForbiddenException.class, () -> useCase.execute(() -> context(Set.of(RoleCodes.CLIENT))));
    }

    private static UseCaseContext context(Set<String> roleCodes) {
        return new UseCaseContext(
                UUID.randomUUID(),
                Set.of(),
                "req_test",
                roleCodes,
                "127.0.0.1",
                "test");
    }

    private static AuthorizationPort auth() {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
                throw new UnsupportedOperationException("Not used");
            }

            @Override
            public boolean hasRole(UseCaseContext context, String roleCode) {
                return context.actorRoleCodes().contains(roleCode);
            }
        };
    }

    private static TransactionManager tx() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(TransactionalWork<T> work) {
                return work.execute();
            }
        };
    }
}
