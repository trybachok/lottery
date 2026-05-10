package com.lottery.application.dto;

import java.util.List;

public record HomePageDto(
        UiTemplateDto template,
        UiThemeDto defaultTheme,
        List<UiThemeDto> themes) {
    public HomePageDto {
        themes = List.copyOf(themes);
    }
}
