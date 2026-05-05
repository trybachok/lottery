<script setup lang="ts">
defineProps<{
  message?: string
  title?: string
}>()
</script>

<template>
  <section v-if="message" class="app-error-message" role="alert">
    <strong v-if="title" class="app-error-message__title">{{ title }}</strong>
    <span>{{ message }}</span>
  </section>
</template>

<style scoped>
.app-error-message {
  display: grid;
  gap: 4px;
  border: 1px solid color-mix(in srgb, var(--color-danger) 26%, transparent);
  border-radius: var(--radius-md);
  background: var(--color-danger-soft);
  color: var(--color-danger);
  padding: 12px 14px;
  font-size: 0.875rem;
}

.app-error-message__title {
  font-weight: 700;
}
</style>
