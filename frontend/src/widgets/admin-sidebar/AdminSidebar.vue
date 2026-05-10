<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import {
  adminNavigationItems,
  filterAdminNavigation,
  type AdminNavigationItem,
} from './adminNavigation'

const props = withDefaults(
  defineProps<{
    userPermissions?: string[]
    roleCodes?: string[]
    items?: AdminNavigationItem[]
  }>(),
  {
    userPermissions: () => [],
    roleCodes: () => [],
    items: () => adminNavigationItems,
  },
)

const route = useRoute()
const visibleItems = computed(() =>
  filterAdminNavigation(props.items, props.userPermissions, props.roleCodes),
)

function isActive(item: AdminNavigationItem): boolean {
  if (item.to === '/admin') {
    return route.path === '/admin'
  }

  return route.path.startsWith(item.to)
}
</script>

<template>
  <aside class="admin-sidebar" aria-label="Admin navigation">
    <RouterLink class="admin-sidebar__brand" to="/admin">Lottery Admin</RouterLink>

    <nav v-if="visibleItems.length > 0" class="admin-sidebar__nav">
      <RouterLink
        v-for="item in visibleItems"
        :key="item.to"
        class="admin-sidebar__link"
        :class="{ 'admin-sidebar__link--active': isActive(item) }"
        :to="item.to"
        :aria-current="isActive(item) ? 'page' : undefined"
      >
        {{ item.label }}
      </RouterLink>
    </nav>

    <AppErrorMessage
      v-else
      title="No admin access"
      message="Your account has no visible admin sections."
    />
  </aside>
</template>

<style scoped>
.admin-sidebar {
  display: grid;
  align-content: start;
  gap: 20px;
  min-height: 100vh;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
  padding: 20px 14px;
}

.admin-sidebar__brand {
  color: var(--color-text);
  font-size: 1rem;
  font-weight: 800;
  text-decoration: none;
}

.admin-sidebar__nav {
  display: grid;
  gap: 4px;
}

.admin-sidebar__link {
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  font-size: 0.9375rem;
  font-weight: 700;
  padding: 10px 12px;
  text-decoration: none;
}

.admin-sidebar__link:hover {
  background: var(--color-surface-muted);
  color: var(--color-surface-muted-text);
}

.admin-sidebar__link--active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.admin-sidebar__link--active:hover {
  color: var(--color-surface-muted-text);
}
</style>
