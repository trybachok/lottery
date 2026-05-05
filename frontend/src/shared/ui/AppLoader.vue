<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    size?: 'sm' | 'md'
  }>(),
  {
    label: 'Loading...',
    size: 'md',
  },
)
</script>

<template>
  <div class="app-loader" :class="`app-loader--${size}`" role="status" aria-live="polite">
    <span class="app-loader__spinner" aria-hidden="true" />
    <span>{{ label }}</span>
  </div>
</template>

<style scoped>
.app-loader {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-muted);
  font-weight: 600;
}

.app-loader--sm {
  font-size: 0.8125rem;
}

.app-loader--md {
  font-size: 0.9375rem;
}

.app-loader__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-border-strong);
  border-top-color: var(--color-primary);
  border-radius: 999px;
  animation: loader-spin 800ms linear infinite;
}

@keyframes loader-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
