package com.lottery.application.usecase.system;

import com.lottery.application.ValidationException;
import java.util.Map;
import java.util.Set;

final class HomePageLayoutValidator {
    private static final Set<String> REQUIRED_REGIONS = Set.of("header", "banner", "sidebar", "main", "footer");

    void validate(Map<String, Object> layout) {
        if (layout == null || layout.isEmpty()) {
            throw new ValidationException("Home page layout must not be empty");
        }
        Object version = layout.get("version");
        if (!(version instanceof Number)) {
            throw new ValidationException("Home page layout version is required");
        }
        Object regionsValue = layout.get("regions");
        if (!(regionsValue instanceof Map<?, ?> regions)) {
            throw new ValidationException("Home page layout regions are required");
        }
        for (String regionName : REQUIRED_REGIONS) {
            Object regionValue = regions.get(regionName);
            if (!(regionValue instanceof Map<?, ?> region)) {
                throw new ValidationException("Home page layout region is required: " + regionName);
            }
            Object type = region.get("type");
            if (!regionName.equals(type)) {
                throw new ValidationException("Home page layout region type mismatch: " + regionName);
            }
        }
    }
}
