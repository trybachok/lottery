import { hasPermission, type PermissionMode } from '@/shared/lib/permissions/hasPermission'
import { AdminPanelPermissions, PermissionCodes } from '@/shared/lib/permissions/permissionCodes'

export type SiteNavigationItem = {
  label: string
  to: string
  requiresAuth?: boolean
  publicOnly?: boolean
  adminOnly?: boolean
  permissions?: string[]
  permissionMode?: PermissionMode
}

export const siteNavigationItems: SiteNavigationItem[] = [
  {
    label: 'Home',
    to: '/',
  },
  {
    label: 'Draws',
    to: '/draws',
    requiresAuth: true,
    permissions: [PermissionCodes.DRAW_READ],
  },
  {
    label: 'Account',
    to: '/account',
    requiresAuth: true,
  },
  {
    label: 'Admin',
    to: '/admin',
    requiresAuth: true,
    permissions: [...AdminPanelPermissions],
    permissionMode: 'any',
  },
  {
    label: 'Docs',
    to: '/docs',
    requiresAuth: true,
    adminOnly: true,
  },
]

export function filterSiteNavigation(
  items: SiteNavigationItem[],
  isAuthenticated: boolean,
  userPermissions: string[],
  roleCodes: string[],
): SiteNavigationItem[] {
  return items.filter((item) => {
    if (item.publicOnly && isAuthenticated) {
      return false
    }

    if (item.requiresAuth && !isAuthenticated) {
      return false
    }

    if (item.adminOnly && !roleCodes.includes('ADMIN')) {
      return false
    }

    if (roleCodes.includes('ADMIN')) {
      return true
    }

    return hasPermission(userPermissions, item.permissions, item.permissionMode ?? 'all')
  })
}
