package com.lottery.application.mapper;

import com.lottery.application.dto.UiTemplateDto;
import com.lottery.application.dto.UiThemeDto;
import com.lottery.domain.model.UiTemplate;
import com.lottery.domain.model.UiTheme;

public final class UiMapper {
    public UiThemeDto toDto(UiTheme theme) {
        return new UiThemeDto(theme.id(), theme.name(), theme.tokens(), theme.defaultTheme(), theme.createdAt());
    }

    public UiTemplateDto toDto(UiTemplate template) {
        return new UiTemplateDto(template.id(), template.name(), template.layout(), template.createdAt());
    }
}
