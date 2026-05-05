<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'
import BaseButton from '@/shared/ui/BaseButton.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import AdminSidebar from '@/widgets/admin-sidebar/AdminSidebar.vue'

const authStore = useAuthStore()
const router = useRouter()

async function logout(): Promise<void> {
  authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="admin-shell">
    <AdminSidebar :user-permissions="authStore.permissions" :role-codes="authStore.roleCodes" />

    <div class="admin-shell__main">
      <header class="admin-shell__topbar">
        <div>
          <p class="admin-shell__eyebrow">Management</p>
          <h1 class="admin-shell__title">Admin panel</h1>
        </div>
        <BaseButton variant="secondary" size="sm" @click="logout">Logout</BaseButton>
      </header>

      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 240px minmax(0, 1fr);
  background: var(--color-bg);
}

.admin-shell__main {
  min-width: 0;
  padding: 24px;
}

.admin-shell__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.admin-shell__eyebrow,
.admin-shell__title {
  margin: 0;
}

.admin-shell__eyebrow {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 800;
  text-transform: uppercase;
}

.admin-shell__title {
  font-size: 1.5rem;
  line-height: 1.2;
}

@media (max-width: 760px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-shell :deep(.admin-sidebar) {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .admin-shell__main {
    padding: 18px;
  }
}
</style>
