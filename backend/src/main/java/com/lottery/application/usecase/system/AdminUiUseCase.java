package com.lottery.application.usecase.system;

import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.ValidationException;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.HomePageSettingsDto;
import com.lottery.application.dto.UiTemplateDto;
import com.lottery.application.dto.UiThemeDto;
import com.lottery.application.mapper.UiMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.SystemSetting;
import com.lottery.domain.model.UiTemplate;
import com.lottery.domain.model.UiTheme;
import com.lottery.domain.repository.SystemSettingsRepository;
import com.lottery.domain.repository.UiTemplateRepository;
import com.lottery.domain.repository.UiThemeRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdminUiUseCase {
    private static final Logger log = LoggerFactory.getLogger(AdminUiUseCase.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final UiThemeRepository themeRepository;
    private final UiTemplateRepository templateRepository;
    private final SystemSettingsRepository settingsRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final UiMapper mapper;
    private final AuditService auditService;
    private final UiThemeValidator themeValidator;
    private final HomePageLayoutValidator layoutValidator;

    public AdminUiUseCase(
            UiThemeRepository themeRepository,
            UiTemplateRepository templateRepository,
            SystemSettingsRepository settingsRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            UiMapper mapper,
            AuditService auditService) {
        this.themeRepository = themeRepository;
        this.templateRepository = templateRepository;
        this.settingsRepository = settingsRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
        this.themeValidator = new UiThemeValidator();
        this.layoutValidator = new HomePageLayoutValidator();
    }

    public List<UiThemeDto> listThemes(int limit, int offset, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_THEME_MANAGE);
            return themeRepository.findAll(pageSize(limit), Math.max(offset, 0)).stream().map(mapper::toDto).toList();
        });
    }

    public UiThemeDto getTheme(UUID id, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_THEME_MANAGE);
            return mapper.toDto(findTheme(id));
        });
    }

    public UiThemeDto createTheme(String name, Map<String, Object> tokens, boolean defaultTheme, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_THEME_MANAGE);
            validateName(name, "Theme name");
            themeValidator.validate(tokens);
            UiTheme created = themeRepository.save(new UiTheme(DomainIds.newId(), name.trim(), tokens, false, clock.now()));
            if (defaultTheme) {
                themeRepository.setDefault(created.id());
                created = findTheme(created.id());
            }
            UiThemeDto dto = mapper.toDto(created);
            auditService.recordChange(context, "UI_THEME_CREATE", "UI_THEME", dto.id(), null, dto);
            log.info("requestId={} actorUserId={} themeId={} ui_theme_created", context.requestId(), context.actorUserId(), dto.id());
            return dto;
        });
    }

    public UiThemeDto updateTheme(UUID id, String name, Map<String, Object> tokens, boolean defaultTheme, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_THEME_MANAGE);
            UiTheme current = findTheme(id);
            validateName(name, "Theme name");
            themeValidator.validate(tokens);
            UiTheme updated = themeRepository.update(new UiTheme(id, name.trim(), tokens, current.defaultTheme(), current.createdAt()));
            if (defaultTheme) {
                themeRepository.setDefault(id);
                updated = findTheme(id);
            }
            UiThemeDto dto = mapper.toDto(updated);
            auditService.recordChange(context, "UI_THEME_UPDATE", "UI_THEME", id, mapper.toDto(current), dto);
            log.info("requestId={} actorUserId={} themeId={} ui_theme_updated", context.requestId(), context.actorUserId(), id);
            return dto;
        });
    }

    public UiThemeDto setDefaultTheme(UUID id, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_THEME_MANAGE);
            UiTheme current = findTheme(id);
            themeRepository.setDefault(id);
            UiTheme updated = findTheme(id);
            auditService.recordChange(context, "UI_THEME_SET_DEFAULT", "UI_THEME", id, mapper.toDto(current), mapper.toDto(updated));
            log.info("requestId={} actorUserId={} themeId={} ui_theme_default_set", context.requestId(), context.actorUserId(), id);
            return mapper.toDto(updated);
        });
    }

    public List<UiTemplateDto> listTemplates(int limit, int offset, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_TEMPLATE_MANAGE);
            return templateRepository.findAll(pageSize(limit), Math.max(offset, 0)).stream().map(mapper::toDto).toList();
        });
    }

    public UiTemplateDto getTemplate(UUID id, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_TEMPLATE_MANAGE);
            return mapper.toDto(findTemplate(id));
        });
    }

    public UiTemplateDto createTemplate(String name, Map<String, Object> layout, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_TEMPLATE_MANAGE);
            validateName(name, "Template name");
            layoutValidator.validate(layout);
            UiTemplateDto dto = mapper.toDto(templateRepository.save(new UiTemplate(DomainIds.newId(), name.trim(), layout, clock.now())));
            auditService.recordChange(context, "UI_TEMPLATE_CREATE", "UI_TEMPLATE", dto.id(), null, dto);
            log.info("requestId={} actorUserId={} templateId={} ui_template_created", context.requestId(), context.actorUserId(), dto.id());
            return dto;
        });
    }

    public UiTemplateDto updateTemplate(UUID id, String name, Map<String, Object> layout, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.UI_TEMPLATE_MANAGE);
            UiTemplate current = findTemplate(id);
            validateName(name, "Template name");
            layoutValidator.validate(layout);
            UiTemplateDto dto = mapper.toDto(templateRepository.update(new UiTemplate(id, name.trim(), layout, current.createdAt())));
            auditService.recordChange(context, "UI_TEMPLATE_UPDATE", "UI_TEMPLATE", id, mapper.toDto(current), dto);
            log.info("requestId={} actorUserId={} templateId={} ui_template_updated", context.requestId(), context.actorUserId(), id);
            return dto;
        });
    }

    public HomePageSettingsDto getHomePageSettings(UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.SYSTEM_SETTINGS_MANAGE);
            return toHomePageSettings(settingsRepository.findByKey(GetHomePageUseCase.HOME_PAGE_SETTING_KEY)
                    .orElseThrow(() -> new NotFoundException("Home page settings")));
        });
    }

    public HomePageSettingsDto updateHomePageSettings(UUID activeTemplateId, UUID defaultThemeId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.SYSTEM_SETTINGS_MANAGE);
            findTemplate(activeTemplateId);
            findTheme(defaultThemeId);
            SystemSetting current = settingsRepository.findByKey(GetHomePageUseCase.HOME_PAGE_SETTING_KEY).orElse(null);
            SystemSetting updated = settingsRepository.save(new SystemSetting(
                    GetHomePageUseCase.HOME_PAGE_SETTING_KEY,
                    Map.of("activeTemplateId", activeTemplateId.toString(), "defaultThemeId", defaultThemeId.toString()),
                    context.actorUserId(),
                    clock.now()));
            auditService.recordChange(
                    context,
                    "HOME_PAGE_SETTINGS_UPDATE",
                    "SYSTEM_SETTING",
                    activeTemplateId,
                    current == null ? null : current.value(),
                    updated.value());
            log.info(
                    "requestId={} actorUserId={} templateId={} defaultThemeId={} home_page_settings_updated",
                    context.requestId(),
                    context.actorUserId(),
                    activeTemplateId,
                    defaultThemeId);
            return toHomePageSettings(updated);
        });
    }

    private UiTheme findTheme(UUID id) {
        return themeRepository.findById(id).orElseThrow(() -> new NotFoundException("UI theme"));
    }

    private UiTemplate findTemplate(UUID id) {
        return templateRepository.findById(id).orElseThrow(() -> new NotFoundException("UI template"));
    }

    private HomePageSettingsDto toHomePageSettings(SystemSetting setting) {
        return new HomePageSettingsDto(
                readId(setting.value(), "activeTemplateId"),
                readId(setting.value(), "defaultThemeId"),
                setting.updatedBy(),
                setting.updatedAt());
    }

    private UUID readId(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        if (raw instanceof String text && !text.isBlank()) {
            return UUID.fromString(text);
        }
        throw new ValidationException("Home page settings must contain " + key);
    }

    private int pageSize(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private void validateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
    }
}
