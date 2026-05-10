package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lottery.application.audit.AuditService;
import com.lottery.application.mapper.UiMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.system.AdminUiUseCase;
import com.lottery.application.usecase.system.GetHomePageUseCase;
import com.lottery.domain.model.AuditLog;
import com.lottery.domain.model.Permission;
import com.lottery.domain.model.Role;
import com.lottery.domain.model.SystemSetting;
import com.lottery.domain.model.UiTemplate;
import com.lottery.domain.model.UiTheme;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.SystemSettingsRepository;
import com.lottery.domain.repository.UiTemplateRepository;
import com.lottery.domain.repository.UiThemeRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AdminUiUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");
    private static final UUID TEMPLATE_ID = UUID.fromString("10000000-0000-0000-0000-000000000010");
    private static final UUID LIGHT_THEME_ID = UUID.fromString("10000000-0000-0000-0000-000000000020");
    private static final UUID DARK_THEME_ID = UUID.fromString("10000000-0000-0000-0000-000000000021");

    @Test
    void getHomePageReturnsActiveTemplateDefaultThemeAndAvailableThemes() {
        Fixture fixture = new Fixture();

        var homePage = fixture.homePageUseCase.get();

        assertEquals(TEMPLATE_ID, homePage.template().id());
        assertEquals(LIGHT_THEME_ID, homePage.defaultTheme().id());
        assertEquals(2, homePage.themes().size());
    }

    @Test
    void rejectsTemplateWithoutRequiredRegions() {
        Fixture fixture = new Fixture();
        Map<String, Object> invalidLayout = Map.of("version", 1, "regions", Map.of("header", Map.of("type", "header")));

        assertThrows(ValidationException.class, () -> fixture.adminUseCase.createTemplate(
                "Broken",
                invalidLayout,
                adminContext(PermissionCodes.UI_TEMPLATE_MANAGE)));
    }

    @Test
    void rejectsThemeWithoutRequiredColorTokens() {
        Fixture fixture = new Fixture();
        Map<String, Object> invalidTokens = Map.of("mode", "light", "colors", Map.of("background", "#fff"));

        assertThrows(ValidationException.class, () -> fixture.adminUseCase.createTheme(
                "Broken",
                invalidTokens,
                false,
                adminContext(PermissionCodes.UI_THEME_MANAGE)));
    }

    @Test
    void updatesHomePageSettingsAndRecordsAudit() {
        Fixture fixture = new Fixture();

        var settings = fixture.adminUseCase.updateHomePageSettings(
                TEMPLATE_ID,
                DARK_THEME_ID,
                adminContext(PermissionCodes.SYSTEM_SETTINGS_MANAGE));

        assertEquals(DARK_THEME_ID, settings.defaultThemeId());
        assertEquals("HOME_PAGE_SETTINGS_UPDATE", fixture.auditLogs.items.getFirst().action());
        assertTrue(fixture.settings.values.get("home_page").value().containsKey("activeTemplateId"));
    }

    @Test
    void requiresThemeManagePermission() {
        Fixture fixture = new Fixture();

        assertThrows(ForbiddenException.class, () -> fixture.adminUseCase.listThemes(20, 0, adminContext(PermissionCodes.DRAW_READ)));
    }

    private static UseCaseContext adminContext(String permission) {
        return new UseCaseContext(
                UUID.randomUUID(),
                Set.of(permission),
                "req_test",
                Set.of(RoleCodes.ADMIN),
                "127.0.0.1",
                "test");
    }

    private static Map<String, Object> validThemeTokens(String mode) {
        return Map.of(
                "mode",
                mode,
                "colors",
                Map.of(
                        "background", "#ffffff",
                        "surface", "#ffffff",
                        "text", "#111111",
                        "primary", "#166534"));
    }

    private static Map<String, Object> validLayout() {
        return Map.of(
                "version",
                1,
                "regions",
                Map.of(
                        "header", Map.of("type", "header"),
                        "banner", Map.of("type", "banner"),
                        "sidebar", Map.of("type", "sidebar"),
                        "main", Map.of("type", "main"),
                        "footer", Map.of("type", "footer")));
    }

    private static AuthorizationPort auth() {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
                if (!context.permissions().contains(permissionCode)) {
                    throw new ForbiddenException(permissionCode);
                }
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

    private static DomainClock clock() {
        return () -> NOW;
    }

    private static final class Fixture {
        private final Themes themes = new Themes();
        private final Templates templates = new Templates();
        private final Settings settings = new Settings();
        private final AuditLogs auditLogs = new AuditLogs();
        private final UiMapper mapper = new UiMapper();
        private final AdminUiUseCase adminUseCase = new AdminUiUseCase(
                themes,
                templates,
                settings,
                auth(),
                tx(),
                clock(),
                mapper,
                new AuditService(auditLogs, new EmptyRbac(), clock()));
        private final GetHomePageUseCase homePageUseCase = new GetHomePageUseCase(templates, themes, settings, tx(), mapper);

        private Fixture() {
            themes.items.put(LIGHT_THEME_ID, new UiTheme(LIGHT_THEME_ID, "Light", validThemeTokens("light"), true, NOW));
            themes.items.put(DARK_THEME_ID, new UiTheme(DARK_THEME_ID, "Dark", validThemeTokens("dark"), false, NOW));
            templates.items.put(TEMPLATE_ID, new UiTemplate(TEMPLATE_ID, "Default", validLayout(), NOW));
            settings.values.put("home_page", new SystemSetting(
                    "home_page",
                    Map.of("activeTemplateId", TEMPLATE_ID.toString(), "defaultThemeId", LIGHT_THEME_ID.toString()),
                    null,
                    NOW));
        }
    }

    private static final class Themes implements UiThemeRepository {
        private final Map<UUID, UiTheme> items = new HashMap<>();

        @Override
        public List<UiTheme> findAll(int limit, int offset) {
            return items.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public Optional<UiTheme> findById(UUID id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<UiTheme> findDefault() {
            return items.values().stream().filter(UiTheme::defaultTheme).findFirst();
        }

        @Override
        public UiTheme save(UiTheme theme) {
            items.put(theme.id(), theme);
            return theme;
        }

        @Override
        public UiTheme update(UiTheme theme) {
            items.put(theme.id(), theme);
            return theme;
        }

        @Override
        public void setDefault(UUID id) {
            List<UiTheme> updated = items.values().stream()
                    .map(theme -> new UiTheme(theme.id(), theme.name(), theme.tokens(), theme.id().equals(id), theme.createdAt()))
                    .toList();
            items.clear();
            updated.forEach(theme -> items.put(theme.id(), theme));
        }
    }

    private static final class Templates implements UiTemplateRepository {
        private final Map<UUID, UiTemplate> items = new HashMap<>();

        @Override
        public List<UiTemplate> findAll(int limit, int offset) {
            return items.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public Optional<UiTemplate> findById(UUID id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public UiTemplate save(UiTemplate template) {
            items.put(template.id(), template);
            return template;
        }

        @Override
        public UiTemplate update(UiTemplate template) {
            items.put(template.id(), template);
            return template;
        }
    }

    private static final class Settings implements SystemSettingsRepository {
        private final Map<String, SystemSetting> values = new HashMap<>();

        @Override
        public Optional<SystemSetting> findByKey(String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public SystemSetting save(SystemSetting setting) {
            values.put(setting.key(), setting);
            return setting;
        }
    }

    private static final class AuditLogs implements AuditLogRepository {
        private final List<AuditLog> items = new ArrayList<>();

        @Override
        public void append(AuditLog auditLog) {
            items.add(auditLog);
        }

        @Override
        public List<AuditLog> find(
                UUID actorUserId,
                String action,
                String entityType,
                UUID entityId,
                Instant createdFrom,
                Instant createdTo,
                int limit,
                int offset) {
            return items.stream().skip(offset).limit(limit).toList();
        }
    }

    private static final class EmptyRbac implements RbacRepository {
        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            return Set.of();
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
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
        public Role saveRole(Role role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Role updateRole(Role role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteRole(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Permission> findAllPermissions(int limit, int offset) {
            return List.of();
        }

        @Override
        public Optional<Permission> findPermissionById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Permission savePermission(Permission permission) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Permission updatePermission(Permission permission) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deletePermission(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Role> findRolesByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public void assignRole(UUID userId, UUID roleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeRole(UUID userId, UUID roleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Permission> findPermissionsByRoleId(UUID roleId) {
            return List.of();
        }

        @Override
        public void assignPermission(UUID roleId, UUID permissionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removePermission(UUID roleId, UUID permissionId) {
            throw new UnsupportedOperationException();
        }
    }
}
