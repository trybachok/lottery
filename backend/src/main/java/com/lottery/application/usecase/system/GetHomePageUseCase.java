package com.lottery.application.usecase.system;

import com.lottery.application.NotFoundException;
import com.lottery.application.dto.HomePageDto;
import com.lottery.application.mapper.UiMapper;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.SystemSetting;
import com.lottery.domain.model.UiTemplate;
import com.lottery.domain.model.UiTheme;
import com.lottery.domain.repository.SystemSettingsRepository;
import com.lottery.domain.repository.UiTemplateRepository;
import com.lottery.domain.repository.UiThemeRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GetHomePageUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetHomePageUseCase.class);
    static final String HOME_PAGE_SETTING_KEY = "home_page";
    private static final int THEME_LIMIT = 100;

    private final UiTemplateRepository templateRepository;
    private final UiThemeRepository themeRepository;
    private final SystemSettingsRepository settingsRepository;
    private final TransactionManager transactionManager;
    private final UiMapper mapper;

    public GetHomePageUseCase(
            UiTemplateRepository templateRepository,
            UiThemeRepository themeRepository,
            SystemSettingsRepository settingsRepository,
            TransactionManager transactionManager,
            UiMapper mapper) {
        this.templateRepository = templateRepository;
        this.themeRepository = themeRepository;
        this.settingsRepository = settingsRepository;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public HomePageDto get() {
        return transactionManager.inTransaction(() -> {
            SystemSetting setting = settingsRepository.findByKey(HOME_PAGE_SETTING_KEY)
                    .orElseThrow(() -> new NotFoundException("Home page settings"));
            UUID templateId = readId(setting.value(), "activeTemplateId");
            UUID defaultThemeId = readId(setting.value(), "defaultThemeId");
            UiTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new NotFoundException("Home page template"));
            UiTheme defaultTheme = themeRepository.findById(defaultThemeId)
                    .or(() -> themeRepository.findDefault())
                    .orElseThrow(() -> new NotFoundException("Default UI theme"));
            List<UiTheme> themes = themeRepository.findAll(THEME_LIMIT, 0);
            log.debug("home_page_loaded templateId={} defaultThemeId={} themeCount={}", template.id(), defaultTheme.id(), themes.size());
            return new HomePageDto(
                    mapper.toDto(template),
                    mapper.toDto(defaultTheme),
                    themes.stream().map(mapper::toDto).toList());
        });
    }

    private UUID readId(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        if (raw instanceof String text && !text.isBlank()) {
            return UUID.fromString(text);
        }
        throw new NotFoundException("Home page " + key);
    }
}
