package com.lottery.domain.repository;

import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import com.lottery.domain.model.Permission;
import com.lottery.domain.model.Role;

public interface RbacRepository {
    Set<String> findPermissionCodesByUserId(UUID userId);

    Set<String> findRoleCodesByUserId(UUID userId);

    void assignRoleByCode(UUID userId, String roleCode);

    default List<Role> findAllRoles(int limit, int offset) {
        throw new UnsupportedOperationException("findAllRoles is not implemented");
    }

    default Optional<Role> findRoleById(UUID id) {
        throw new UnsupportedOperationException("findRoleById is not implemented");
    }

    default Role saveRole(Role role) {
        throw new UnsupportedOperationException("saveRole is not implemented");
    }

    default Role updateRole(Role role) {
        throw new UnsupportedOperationException("updateRole is not implemented");
    }

    default void deleteRole(UUID id) {
        throw new UnsupportedOperationException("deleteRole is not implemented");
    }

    default List<Permission> findAllPermissions(int limit, int offset) {
        throw new UnsupportedOperationException("findAllPermissions is not implemented");
    }

    default Optional<Permission> findPermissionById(UUID id) {
        throw new UnsupportedOperationException("findPermissionById is not implemented");
    }

    default Permission savePermission(Permission permission) {
        throw new UnsupportedOperationException("savePermission is not implemented");
    }

    default Permission updatePermission(Permission permission) {
        throw new UnsupportedOperationException("updatePermission is not implemented");
    }

    default void deletePermission(UUID id) {
        throw new UnsupportedOperationException("deletePermission is not implemented");
    }

    default List<Role> findRolesByUserId(UUID userId) {
        throw new UnsupportedOperationException("findRolesByUserId is not implemented");
    }

    default void assignRole(UUID userId, UUID roleId) {
        throw new UnsupportedOperationException("assignRole is not implemented");
    }

    default void removeRole(UUID userId, UUID roleId) {
        throw new UnsupportedOperationException("removeRole is not implemented");
    }

    default List<Permission> findPermissionsByRoleId(UUID roleId) {
        throw new UnsupportedOperationException("findPermissionsByRoleId is not implemented");
    }

    default void assignPermission(UUID roleId, UUID permissionId) {
        throw new UnsupportedOperationException("assignPermission is not implemented");
    }

    default void removePermission(UUID roleId, UUID permissionId) {
        throw new UnsupportedOperationException("removePermission is not implemented");
    }
}
