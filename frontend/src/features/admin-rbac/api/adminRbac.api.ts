import {
  adminAssignRolePermission,
  adminAssignUserRole,
  adminCreatePermission,
  adminCreateRole,
  adminCreateUser,
  adminDeletePermission,
  adminDeleteRole,
  adminDeleteUser,
  adminListPermissions,
  adminListRolePermissions,
  adminListRoles,
  adminListUserRoles,
  adminListUsers,
  adminRemoveRolePermission,
  adminRemoveUserRole,
  adminUpdatePermission,
  adminUpdateRole,
  adminUpdateUser,
} from '@/shared/api/generated/sdk.gen'
import type {
  AdminUserRequestWritable,
  Permission,
  PermissionRequest,
  Role,
  RoleRequest,
  User,
} from '@/shared/api/generated/types.gen'

export async function listAdminUsers(): Promise<User[]> {
  const response = await adminListUsers({
    query: { limit: 100, offset: 0 },
    throwOnError: true,
  })

  return response.data.items
}

export async function createAdminUser(request: AdminUserRequestWritable): Promise<User> {
  const response = await adminCreateUser({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function updateAdminUser(userId: string, request: AdminUserRequestWritable): Promise<User> {
  const response = await adminUpdateUser({
    path: { userId },
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function deleteAdminUser(userId: string): Promise<void> {
  await adminDeleteUser({
    path: { userId },
    throwOnError: true,
  })
}

export async function listAdminRoles(): Promise<Role[]> {
  const response = await adminListRoles({
    query: { limit: 100, offset: 0 },
    throwOnError: true,
  })

  return response.data.items
}

export async function createAdminRole(request: RoleRequest): Promise<Role> {
  const response = await adminCreateRole({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function updateAdminRole(roleId: string, request: RoleRequest): Promise<Role> {
  const response = await adminUpdateRole({
    path: { roleId },
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function deleteAdminRole(roleId: string): Promise<void> {
  await adminDeleteRole({
    path: { roleId },
    throwOnError: true,
  })
}

export async function listAdminPermissions(): Promise<Permission[]> {
  const response = await adminListPermissions({
    throwOnError: true,
  })

  return response.data.items
}

export async function createAdminPermission(request: PermissionRequest): Promise<Permission> {
  const response = await adminCreatePermission({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function updateAdminPermission(permissionId: string, request: PermissionRequest): Promise<Permission> {
  const response = await adminUpdatePermission({
    path: { permissionId },
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function deleteAdminPermission(permissionId: string): Promise<void> {
  await adminDeletePermission({
    path: { permissionId },
    throwOnError: true,
  })
}

export async function listUserRoles(userId: string): Promise<Role[]> {
  const response = await adminListUserRoles({
    path: { userId },
    throwOnError: true,
  })

  return response.data.items
}

export async function assignUserRole(userId: string, roleId: string): Promise<void> {
  await adminAssignUserRole({
    path: { userId },
    body: { roleId },
    throwOnError: true,
  })
}

export async function removeUserRole(userId: string, roleId: string): Promise<void> {
  await adminRemoveUserRole({
    path: { userId, roleId },
    throwOnError: true,
  })
}

export async function listRolePermissions(roleId: string): Promise<Permission[]> {
  const response = await adminListRolePermissions({
    path: { roleId },
    throwOnError: true,
  })

  return response.data.items
}

export async function assignRolePermission(roleId: string, permissionId: string): Promise<void> {
  await adminAssignRolePermission({
    path: { roleId },
    body: { permissionId },
    throwOnError: true,
  })
}

export async function removeRolePermission(roleId: string, permissionId: string): Promise<void> {
  await adminRemoveRolePermission({
    path: { roleId, permissionId },
    throwOnError: true,
  })
}
