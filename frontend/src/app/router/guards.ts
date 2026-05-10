import type { Router } from 'vue-router'
import { useAuthStore } from '@/features/auth/model/auth.store'
import { hasPermission, type PermissionMode } from '@/shared/lib/permissions/hasPermission'

const defaultAuthenticatedPath = '/draws'
const loginPath = '/login'
const forbiddenPath = '/forbidden'

export function registerRouterGuards(router: Router): void {
  router.beforeEach((to) => {
    const authStore = useAuthStore()
    const routeMetaChain = to.matched.map((route) => route.meta)
    const requiresAuth = routeMetaChain.some((meta) => Boolean(meta.requiresAuth))
    const publicOnly = routeMetaChain.some((meta) => Boolean(meta.publicOnly))

    if (publicOnly && authStore.isAuthenticated) {
      return defaultAuthenticatedPath
    }

    if (!requiresAuth) {
      return true
    }

    if (!authStore.isAuthenticated) {
      return {
        path: loginPath,
        query: {
          redirect: to.fullPath,
        },
      }
    }

    const hasAdminRole = authStore.roleCodes.includes('ADMIN')

    if (routeMetaChain.some((meta) => Boolean(meta.adminOnly)) && !hasAdminRole) {
      return {
        path: forbiddenPath,
        query: {
          from: to.fullPath,
        },
      }
    }

    if (!hasAdminRole && !hasRequiredRoutePermissions(routeMetaChain, authStore.permissions)) {
      return {
        path: forbiddenPath,
        query: {
          from: to.fullPath,
        },
      }
    }

    return true
  })
}

function hasRequiredRoutePermissions(
  routeMetaChain: Array<{ permissions?: string[]; permissionMode?: PermissionMode }>,
  userPermissions: string[],
): boolean {
  return routeMetaChain.every((meta) =>
    hasPermission(userPermissions, meta.permissions, meta.permissionMode ?? 'all'),
  )
}

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    publicOnly?: boolean
    adminOnly?: boolean
    permissions?: string[]
    permissionMode?: PermissionMode
  }
}
