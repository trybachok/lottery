import { hasPermission } from '@/shared/lib/permissions/hasPermission'

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
    permissions: ['user.manage', 'draw.create', 'report.draw.export', 'audit.read'],
    permissionMode: 'any',
  },
  {
    label: 'Users',
    to: '/admin/users',
    permissions: ['user.manage'],
  },
  {
    label: 'Roles',
    to: '/admin/roles',
    permissions: ['role.manage', 'permission.manage'],
    permissionMode: 'any',
  },
  {
    label: 'Draws',
    to: '/admin/draws',
    permissions: ['draw.create', 'draw.run'],
    permissionMode: 'any',
  },
  {
    label: 'Reports',
    to: '/admin/reports',
    permissions: ['report.draw.export', 'report.ticket.export'],
    permissionMode: 'any',
  },
  {
    label: 'Audit',
    to: '/admin/audit-logs',
    permissions: ['audit.read'],
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
