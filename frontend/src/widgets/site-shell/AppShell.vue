<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import BaseButton from '@/shared/ui/BaseButton.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import { filterSiteNavigation, siteNavigationItems } from './siteNavigation'

const authStore = useAuthStore()
const router = useRouter()

const visibleNavigationItems = computed(() =>
  filterSiteNavigation(
    siteNavigationItems,
    authStore.isAuthenticated,
    authStore.permissions,
    authStore.roleCodes,
  ),
)

async function logout(): Promise<void> {
  authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header class="app-shell__header">
      <RouterLink class="app-shell__brand" to="/">
        <span class="app-shell__brand-mark" aria-hidden="true">L</span>
        <span class="app-shell__brand-text">Lottery</span>
      </RouterLink>

      <nav class="app-shell__nav" aria-label="Site navigation">
        <RouterLink
          v-for="item in visibleNavigationItems"
          :key="item.to"
          class="app-shell__nav-link"
          :to="item.to"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="app-shell__user">
        <span v-if="authStore.user" class="app-shell__user-name">{{ authStore.user.login }}</span>
        <BaseButton v-if="authStore.isAuthenticated" variant="secondary" size="sm" @click="logout">Logout</BaseButton>
        <template v-else>
          <RouterLink class="app-shell__auth-link" to="/login">Login</RouterLink>
          <RouterLink class="app-shell__auth-link app-shell__auth-link--primary" to="/register">
            Register
          </RouterLink>
        </template>
      </div>
    </header>

    <main class="app-shell__main">
      <RouterView />
    </main>

    <footer class="app-shell__footer">
      <span>Lottery system</span>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  min-height: 100vh;
  grid-template-rows: auto 1fr auto;
  background: var(--color-bg);
  color: var(--color-text);
}

.app-shell__header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 20px;
  border-bottom: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  padding: 12px clamp(16px, 4vw, 40px);
  backdrop-filter: blur(12px);
}

.app-shell__brand,
.app-shell__nav,
.app-shell__user {
  display: flex;
  align-items: center;
}

.app-shell__brand {
  gap: 10px;
  color: inherit;
  text-decoration: none;
}

.app-shell__brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-text, #fff);
  font-weight: 800;
}

.app-shell__brand-text {
  font-weight: 800;
}

.app-shell__nav {
  min-width: 0;
  flex-wrap: wrap;
  gap: 6px;
}

.app-shell__nav-link,
.app-shell__auth-link {
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  font-weight: 700;
  text-decoration: none;
}

.app-shell__nav-link {
  padding: 9px 10px;
}

.app-shell__nav-link:hover,
.app-shell__nav-link.router-link-active {
  background: var(--color-surface-muted);
  color: var(--color-surface-muted-text);
}

.app-shell__user {
  justify-content: flex-end;
  gap: 10px;
}

.app-shell__user-name {
  max-width: 180px;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-shell__auth-link {
  padding: 9px 12px;
}

.app-shell__auth-link--primary {
  background: var(--color-primary);
  color: var(--color-primary-text, #fff);
}

.app-shell__main {
  min-width: 0;
}

.app-shell__footer {
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  padding: 14px clamp(16px, 4vw, 40px);
  font-size: 0.875rem;
}

@media (max-width: 860px) {
  .app-shell__header {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .app-shell__user {
    justify-content: flex-start;
  }
}
</style>
