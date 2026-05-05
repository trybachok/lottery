import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { registerRouterGuards } from './guards'

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
    },
    {
        path: '/account',
        component: () => import('@/pages/account/AccountPage.vue'),
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
                'user.manage',
                'role.manage',
                'permission.manage',
                'draw.create',
                'draw.run',
                'report.draw.export',
                'report.ticket.export',
                'audit.read',
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
                component: () => import('@/pages/admin/sections/AdminSectionPlaceholder.vue'),
                props: {
                    title: 'Users',
                    description: 'User management tools will be implemented in the admin users phase.',
                },
                meta: {
                    permissions: ['user.manage'],
                },
            },
            {
                path: 'roles',
                component: () => import('@/pages/admin/sections/AdminSectionPlaceholder.vue'),
                props: {
                    title: 'Roles and permissions',
                    description: 'Role and permission management tools will be implemented in the RBAC phase.',
                },
                meta: {
                    permissions: ['role.manage', 'permission.manage'],
                    permissionMode: 'any',
                },
            },
            {
                path: 'draws',
                component: () => import('@/pages/admin/sections/AdminDrawsPage.vue'),
                meta: {
                    permissions: ['draw.create', 'draw.update', 'draw.run'],
                    permissionMode: 'any',
                },
            },
            {
                path: 'reports',
                component: () => import('@/pages/admin/sections/AdminSectionPlaceholder.vue'),
                props: {
                    title: 'Reports',
                    description: 'Draw and ticket reports will be implemented in the reports phase.',
                },
                meta: {
                    permissions: ['report.draw.export', 'report.ticket.export'],
                    permissionMode: 'any',
                },
            },
            {
                path: 'audit-logs',
                component: () => import('@/pages/admin/sections/AdminSectionPlaceholder.vue'),
                props: {
                    title: 'Audit logs',
                    description: 'Administrative audit log browsing will be implemented in the audit phase.',
                },
                meta: {
                    permissions: ['audit.read'],
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
