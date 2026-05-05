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
        component: () => import('@/pages/admin/AdminPage.vue'),
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
