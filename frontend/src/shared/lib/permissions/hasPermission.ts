export type PermissionMode = 'all' | 'any'

export function hasPermission(
  userPermissions: readonly string[],
  requiredPermissions: readonly string[] | undefined,
  mode: PermissionMode = 'all',
): boolean {
  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true
  }

  if (userPermissions.includes('*')) {
    return true
  }

  if (mode === 'any') {
    return requiredPermissions.some((permission) => userPermissions.includes(permission))
  }

  return requiredPermissions.every((permission) => userPermissions.includes(permission))
}
