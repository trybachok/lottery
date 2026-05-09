import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { registerRouterGuards } from './guards'
import { PermissionCodes, UserAdminPermissions } from '@/shared/lib/permissions/permissionCodes'

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        component: () => import('@/pages/public/Home.vue'),
    },
    {
        path: '/login',
        component: () => import('@/pages/public/LoginPage.vue'),
        meta: {
            publicOnly: true,
        },
    },
    {
        path: '/register',
        component: () => import('@/pages/public/RegisterPage.vue'),
        meta: {
            publicOnly: true,
        },
    },
    {
        path: '/draws',
        component: () => import('@/pages/public/DrawsPage.vue'),
        meta: {
            requiresAuth: true,
            permissions: [PermissionCodes.DRAW_READ],
        },
    },
    {
        path: '/draws/:drawId',
        component: () => import('@/pages/public/DrawDetailsPage.vue'),
        meta: {
            requiresAuth: true,
            permissions: [PermissionCodes.DRAW_READ],
        },
    },
    {
        path: '/account',
        component: () => import('@/pages/account/AccountPage.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/account/tickets/:ticketId',
        component: () => import('@/pages/account/TicketDetailsPage.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/admin',
        component: () => import('@/pages/admin/ui/AdminShell.vue'),
        meta: {
            requiresAuth: true,
            permissions: [
                ...UserAdminPermissions,
                PermissionCodes.ROLE_READ,
                PermissionCodes.ROLE_MANAGE,
                PermissionCodes.PERMISSION_MANAGE,
                PermissionCodes.DRAW_READ,
                PermissionCodes.DRAW_CREATE,
                PermissionCodes.DRAW_UPDATE,
                PermissionCodes.DRAW_RUN,
                PermissionCodes.REPORT_DRAW_EXPORT,
                PermissionCodes.REPORT_TICKET_EXPORT,
                PermissionCodes.AUDIT_READ,
            ],
            permissionMode: 'any',
        },
        children: [
            {
                path: '',
                component: () => import('@/pages/admin/sections/AdminDashboardPage.vue'),
            },
            {
                path: 'users',
                component: () => import('@/pages/admin/sections/AdminUsersPage.vue'),
                meta: {
                    permissions: [PermissionCodes.USER_READ],
                },
            },
            {
                path: 'roles',
                component: () => import('@/pages/admin/sections/AdminRolesPage.vue'),
                meta: {
                    permissions: [PermissionCodes.ROLE_READ],
                },
            },
            {
                path: 'permissions',
                component: () => import('@/pages/admin/sections/AdminPermissionsPage.vue'),
                meta: {
                    permissions: [PermissionCodes.PERMISSION_MANAGE],
                },
            },
            {
                path: 'draws',
                component: () => import('@/pages/admin/sections/AdminDrawsPage.vue'),
                meta: {
                    permissions: [PermissionCodes.DRAW_READ],
                },
            },
            {
                path: 'reports',
                component: () => import('@/pages/admin/sections/AdminReportsPage.vue'),
                meta: {
                    permissions: [PermissionCodes.REPORT_DRAW_EXPORT, PermissionCodes.REPORT_TICKET_EXPORT],
                    permissionMode: 'any',
                },
            },
            {
                path: 'audit-logs',
                component: () => import('@/pages/admin/sections/AdminAuditLogsPage.vue'),
                meta: {
                    permissions: [PermissionCodes.AUDIT_READ],
                },
            },
        ],
    },
    {
        path: '/forbidden',
        component: () => import('@/pages/public/ForbiddenPage.vue'),
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/draws',
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

registerRouterGuards(router)

export default router
