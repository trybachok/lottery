<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
    size?: 'sm' | 'md'
    type?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
  }>(),
  {
    variant: 'primary',
    size: 'md',
    type: 'button',
    loading: false,
    disabled: false,
  },
)
</script>

<template>
  <button
    class="base-button"
    :class="[`base-button--${variant}`, `base-button--${size}`]"
    :type="type"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="base-button__spinner" aria-hidden="true" />
    <span class="base-button__content">
      <slot />
    </span>
  </button>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  min-width: 40px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 650;
  line-height: 1;
  text-decoration: none;
  transition:
    background-color 140ms ease,
    border-color 140ms ease,
    color 140ms ease,
    box-shadow 140ms ease;
}

.base-button:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--color-focus) 28%, transparent);
  outline-offset: 2px;
}

.base-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.base-button--sm {
  min-height: 34px;
  padding: 0 12px;
  font-size: 0.875rem;
}

.base-button--md {
  min-height: 42px;
  padding: 0 16px;
  font-size: 0.9375rem;
}

.base-button--primary {
  background: var(--color-primary);
  color: white;
}

.base-button--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.base-button--secondary {
  border-color: var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
}

.base-button--secondary:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-muted);
}

.base-button--danger {
  background: var(--color-danger);
  color: white;
}

.base-button--ghost {
  background: transparent;
  color: var(--color-text);
}

.base-button--ghost:hover:not(:disabled) {
  background: var(--color-surface-muted);
}

.base-button__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 999px;
  animation: button-spin 700ms linear infinite;
}

@keyframes button-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
