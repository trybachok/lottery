<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<template>
  <section class="base-card">
    <header v-if="title || description || $slots.actions" class="base-card__header">
      <div class="base-card__heading">
        <h2 v-if="title" class="base-card__title">{{ title }}</h2>
        <p v-if="description" class="base-card__description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="base-card__actions">
        <slot name="actions" />
      </div>
    </header>
    <slot />
  </section>
</template>

<style scoped>
.base-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  padding: 18px;
}

.base-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.base-card__heading {
  display: grid;
  gap: 4px;
}

.base-card__title {
  margin: 0;
  font-size: 1rem;
  line-height: 1.25;
}

.base-card__description {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  line-height: 1.45;
}

.base-card__actions {
  flex: 0 0 auto;
}
</style>
