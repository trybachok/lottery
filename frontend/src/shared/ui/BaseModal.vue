<script setup lang="ts">
withDefaults(
  defineProps<{
    open: boolean
    title?: string
  }>(),
  {
    title: undefined,
  },
)

defineEmits<{
  close: []
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="base-modal" role="presentation">
      <button class="base-modal__backdrop" type="button" aria-label="Close modal" @click="$emit('close')" />
      <section class="base-modal__panel" role="dialog" aria-modal="true" :aria-label="title">
        <header v-if="title || $slots.header" class="base-modal__header">
          <slot name="header">
            <h2 class="base-modal__title">{{ title }}</h2>
          </slot>
          <button class="base-modal__close" type="button" aria-label="Close modal" @click="$emit('close')">
            x
          </button>
        </header>
        <div class="base-modal__body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="base-modal__footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.base-modal {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 20px;
}

.base-modal__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgb(15 23 42 / 52%);
  cursor: pointer;
}

.base-modal__panel {
  position: relative;
  z-index: 1;
  width: min(100%, 520px);
  max-height: min(720px, calc(100vh - 40px));
  overflow: auto;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-md);
}

.base-modal__header,
.base-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
}

.base-modal__header {
  border-bottom: 1px solid var(--color-border);
}

.base-modal__footer {
  border-top: 1px solid var(--color-border);
}

.base-modal__title {
  margin: 0;
  font-size: 1rem;
}

.base-modal__body {
  padding: 18px;
}

.base-modal__close {
  display: inline-grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
}

.base-modal__close:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}
</style>
