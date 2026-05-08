package com.lottery.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MigrationResourceSmokeTest {
    @Test
    void initialSchemaContainsRequiredTablesAndPartitions() throws IOException {
        String migration = resource("/db/migration/V1__initial_schema.sql");

        for (String tableName : requiredTables()) {
            assertTrue(migration.contains("create table " + tableName), "Missing table: " + tableName);
        }

        assertTrue(migration.contains("partition by hash (id)"), "tickets must be partitioned");
        assertTrue(migration.contains("partition by range (received_at)"), "payment_webhook_events must be partitioned");
        assertTrue(migration.contains("partition by range (created_at)"), "audit_logs must be partitioned");
        assertTrue(migration.contains("chk_draws_status"), "draw status check constraint is required");
        assertTrue(migration.contains("chk_tickets_status"), "ticket status check constraint is required");
        assertTrue(migration.contains("chk_invoices_status"), "invoice status check constraint is required");
        assertTrue(migration.contains("chk_payments_status"), "payment status check constraint is required");
    }

    @Test
    void rbacSeedContainsSystemRolesAndAtomicPermissions() throws IOException {
        String migration = resource("/db/migration/V2__seed_rbac.sql");

        for (String roleCode : List.of("ADMIN", "MANAGER", "CLIENT")) {
            assertTrue(migration.contains("'" + roleCode.toLowerCase() + "'"), "Missing role seed: " + roleCode);
        }
        for (String permissionCode : List.of(
                "user.read",
                "user.create",
                "draw.read",
                "draw.create",
                "draw.run",
                "ticket.create",
                "payment.refund",
                "audit.read",
                "system.settings.manage")) {
            assertTrue(migration.contains("'" + permissionCode + "'"), "Missing permission seed: " + permissionCode);
        }
    }

    @Test
    void paymentOutboxMigrationContainsRetryTableAndIndexes() throws IOException {
        String migration = resource("/db/migration/V3__payment_outbox.sql");

        assertTrue(migration.contains("create table payment_outbox"), "payment outbox table is required");
        assertTrue(migration.contains("create_invoice"), "create invoice outbox type is required");
        assertTrue(migration.contains("refund_payment"), "refund outbox type is required");
        assertTrue(migration.contains("idx_payment_outbox_due"), "due index is required for retry worker");
        assertTrue(migration.contains("payment_url"), "invoice payment URL persistence is required");
    }

    private String resource(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertNotNull(inputStream, "Missing migration resource: " + path);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }
    }

    private List<String> requiredTables() {
        return List.of(
                "users",
                "roles",
                "permissions",
                "user_roles",
                "role_permissions",
                "combination_schemas",
                "ui_themes",
                "ui_templates",
                "draws",
                "prizes",
                "tickets",
                "draw_results",
                "winning_rules",
                "invoices",
                "payments",
                "payment_webhook_events",
                "audit_logs",
                "system_settings");
    }
}
