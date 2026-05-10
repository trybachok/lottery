<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/features/theme/model/theme.store'

const themeStore = useThemeStore()
const options = computed(() => themeStore.themes)
</script>

<template>
  <div v-if="options.length > 1" class="theme-switcher" role="group" aria-label="Theme">
    <button
      v-for="theme in options"
      :key="theme.id"
      class="theme-switcher__button"
      :class="{ 'theme-switcher__button--active': themeStore.selectedThemeId === theme.id }"
      type="button"
      :aria-pressed="themeStore.selectedThemeId === theme.id"
      @click="themeStore.selectTheme(theme.id)"
    >
      {{ theme.name }}
    </button>
  </div>
</template>

<style scoped>
.theme-switcher {
  display: inline-grid;
  grid-auto-flow: column;
  gap: 4px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 4px;
}

.theme-switcher__button {
  min-height: 32px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0 10px;
  font-weight: 650;
}

.theme-switcher__button--active {
  background: var(--color-primary);
  color: var(--color-primary-text, #fff);
}
</style>
