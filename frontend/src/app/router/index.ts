import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        component: () => import('@/pages/public/Home.vue'),
    },
    {
        path: '/login',
        component: () => import('@/pages/public/LoginPage.vue'),
    },
    {
        path: '/register',
        component: () => import('@/pages/public/RegisterPage.vue'),
    },
    {
        path: '/draws',
        component: () => import('@/pages/public/DrawsPage.vue'),
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router
