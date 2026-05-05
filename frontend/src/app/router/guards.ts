import type { Router } from 'vue-router'
import { useAuthStore } from '@/features/auth/model/auth.store'
import { hasPermission, type PermissionMode } from '@/shared/lib/permissions/hasPermission'

const defaultAuthenticatedPath = '/draws'
const loginPath = '/login'
const forbiddenPath = '/forbidden'

export function registerRouterGuards(router: Router): void {
  router.beforeEach((to) => {
    const authStore = useAuthStore()
    const requiresAuth = Boolean(to.meta.requiresAuth)
    const publicOnly = Boolean(to.meta.publicOnly)
    const permissions = to.meta.permissions
    const permissionMode = to.meta.permissionMode ?? 'all'

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

    if (
      !authStore.roleCodes.includes('ADMIN') &&
      !hasPermission(authStore.permissions, permissions, permissionMode)
    ) {
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

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    publicOnly?: boolean
    permissions?: string[]
    permissionMode?: PermissionMode
  }
}
