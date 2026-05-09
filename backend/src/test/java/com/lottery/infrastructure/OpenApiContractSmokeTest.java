package com.lottery.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class OpenApiContractSmokeTest {
    @Test
    void listEndpointsUseDtoListResponses() throws IOException {
        String openApi = openApi();

        assertTrue(section(openApi, "  /draws:", "    post:")
                .contains("$ref: '#/components/schemas/DrawListResponse'"));
        assertFalse(section(openApi, "  /draws:", "    post:")
                .contains("$ref: '#/components/schemas/DrawReportResponse'"));

        assertTrue(section(openApi, "  /tickets:", "    post:")
                .contains("$ref: '#/components/schemas/TicketListResponse'"));
        assertFalse(section(openApi, "  /tickets:", "    post:")
                .contains("$ref: '#/components/schemas/TicketReportResponse'"));
    }

    @Test
    void documentedOperationsMatchImplementedBusinessRoutes() throws IOException {
        String openApi = openApi();

        for (String operationId : List.of(
                "getOpenApiSpec",
                "registerUser",
                "loginByPassword",
                "createUser",
                "getDraws",
                "createDraw",
                "getDrawById",
                "updateDraw",
                "activateDraw",
                "pauseDraw",
                "postponeDraw",
                "closeDrawSales",
                "cancelDraw",
                "archiveDraw",
                "runDraw",
                "getDrawResult",
                "getTickets",
                "createTicket",
                "bulkCreateTickets",
                "getTicketById",
                "deleteTicket",
                "checkTicketResult",
                "cancelTicket",
                "getTicketInvoice",
                "createInvoiceForTicket",
                "getInvoiceById",
                "cancelInvoice",
                "expireInvoice",
                "refundPayment",
                "processPaymentWebhook",
                "getDrawReport",
                "getTicketReport",
                "exportDrawReport",
                "exportTicketReport",
                "getAuditLogs",
                "adminListUsers",
                "adminCreateUser",
                "adminGetUser",
                "adminUpdateUser",
                "adminDeleteUser",
                "adminListUserRoles",
                "adminAssignUserRole",
                "adminRemoveUserRole",
                "adminListRoles",
                "adminCreateRole",
                "adminGetRole",
                "adminUpdateRole",
                "adminDeleteRole",
                "adminListRolePermissions",
                "adminAssignRolePermission",
                "adminRemoveRolePermission",
                "adminListPermissions",
                "adminCreatePermission",
                "adminGetPermission",
                "adminUpdatePermission",
                "adminDeletePermission",
                "adminAssignDrawManager")) {
            assertTrue(openApi.contains("operationId: " + operationId), "Missing operationId: " + operationId);
        }

        assertTrue(section(openApi, "  /admin/permissions:", "    post:")
                .contains("$ref: '#/components/parameters/LimitQuery'"));
        assertTrue(section(openApi, "  /admin/permissions:", "    post:")
                .contains("$ref: '#/components/parameters/OffsetQuery'"));
    }

    @Test
    void restBoundaryDoesNotExposeDomainEntitiesAsDtos() throws IOException {
        Path sourceRoot = Path.of(System.getProperty("user.dir"), "src/main/java");

        for (String relativeRoot : List.of("com/lottery/presentation/rest", "com/lottery/application/dto")) {
            Path root = sourceRoot.resolve(relativeRoot);
            try (var files = Files.walk(root)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> assertNoDomainModelImport(path, sourceRoot));
            }
        }
    }

    private void assertNoDomainModelImport(Path path, Path sourceRoot) {
        try {
            String source = Files.readString(path);
            assertFalse(
                    source.contains("import com.lottery.domain.model."),
                    "REST/DTO boundary must not expose domain entities: " + sourceRoot.relativize(path));
        } catch (IOException exception) {
            throw new AssertionError("Cannot read source file: " + path, exception);
        }
    }

    private String openApi() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/openapi/openapi.yaml")) {
            assertNotNull(inputStream, "Missing OpenAPI resource");
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String section(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        assertTrue(startIndex >= 0, "Missing section start: " + start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(endIndex > startIndex, "Missing section end: " + end);
        return source.substring(startIndex, endIndex);
    }
}
