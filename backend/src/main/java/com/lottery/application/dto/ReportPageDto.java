package com.lottery.application.dto;

import java.util.List;

public record ReportPageDto<T>(List<T> items, long total, int limit, int offset, boolean hasMore) {
    public ReportPageDto {
        items = List.copyOf(items);
    }

    public static <T> ReportPageDto<T> of(List<T> items, long total, int limit, int offset) {
        return new ReportPageDto<>(items, total, limit, offset, offset + items.size() < total);
    }
}
