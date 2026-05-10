package com.lottery.application.usecase.system;

import com.lottery.application.ForbiddenException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.openapi.OpenApiDocumentPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GetOpenApiDocumentUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetOpenApiDocumentUseCase.class);

    private final OpenApiDocumentPort openApiDocumentPort;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;

    public GetOpenApiDocumentUseCase(
            OpenApiDocumentPort openApiDocumentPort,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager) {
        this.openApiDocumentPort = openApiDocumentPort;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
    }

    public String execute(Supplier<UseCaseContext> contextSupplier) {
        return transactionManager.inTransaction(() -> {
            UseCaseContext context = contextSupplier.get();
            if (!authorizationPort.hasRole(context, RoleCodes.ADMIN)) {
                log.warn(
                        "requestId={} actorUserId={} openapi_access_denied",
                        context.requestId(),
                        context.actorUserId());
                throw new ForbiddenException(RoleCodes.ADMIN);
            }
            return openApiDocumentPort.load();
        });
    }
}
