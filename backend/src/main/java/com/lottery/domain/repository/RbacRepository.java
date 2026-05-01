package com.lottery.domain.repository;

import java.util.Set;
import java.util.UUID;

public interface RbacRepository {
    Set<String> findPermissionCodesByUserId(UUID userId);

    Set<String> findRoleCodesByUserId(UUID userId);

    void assignRoleByCode(UUID userId, String roleCode);
}
