import { hasPermission } from '@/shared/lib/permissions/hasPermission'
import { PermissionCodes, UserAdminPermissions } from '@/shared/lib/permissions/permissionCodes'

export type AdminNavigationItem = {
  label: string
  to: string
  permissions?: string[]
  permissionMode?: 'all' | 'any'
}

export const adminNavigationItems: AdminNavigationItem[] = [
  {
    label: 'Dashboard',
    to: '/admin',
    permissions: [
      ...UserAdminPermissions,
      PermissionCodes.DRAW_READ,
      PermissionCodes.REPORT_DRAW_EXPORT,
      PermissionCodes.AUDIT_READ,
    ],
    permissionMode: 'any',
  },
  {
    label: 'Users',
    to: '/admin/users',
    permissions: [PermissionCodes.USER_READ],
  },
  {
    label: 'Roles',
    to: '/admin/roles',
    permissions: [PermissionCodes.ROLE_READ],
  },
  {
    label: 'Permissions',
    to: '/admin/permissions',
    permissions: [PermissionCodes.PERMISSION_MANAGE],
  },
  {
    label: 'Draws',
    to: '/admin/draws',
    permissions: [PermissionCodes.DRAW_READ],
  },
  {
    label: 'Reports',
    to: '/admin/reports',
    permissions: [PermissionCodes.REPORT_DRAW_EXPORT, PermissionCodes.REPORT_TICKET_EXPORT],
    permissionMode: 'any',
  },
  {
    label: 'Audit',
    to: '/admin/audit-logs',
    permissions: [PermissionCodes.AUDIT_READ],
  },
]

export function filterAdminNavigation(
  items: AdminNavigationItem[],
  userPermissions: string[],
  roleCodes: string[],
): AdminNavigationItem[] {
  if (roleCodes.includes('ADMIN')) {
    return items
  }

  return items.filter((item) =>
    hasPermission(userPermissions, item.permissions, item.permissionMode ?? 'all'),
  )
}
