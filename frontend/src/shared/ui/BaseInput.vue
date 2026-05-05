<script setup lang="ts">
defineProps<{
  id?: string
  modelValue: string | number
  label?: string
  type?: string
  placeholder?: string
  error?: string
  disabled?: boolean
  autocomplete?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <label class="base-input">
    <span v-if="label" class="base-input__label">{{ label }}</span>
    <input
      :id="id"
      class="base-input__control"
      :class="{ 'base-input__control--invalid': error }"
      :value="modelValue"
      :type="type ?? 'text'"
      :placeholder="placeholder"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :aria-invalid="Boolean(error)"
      :aria-describedby="error && id ? `${id}-error` : undefined"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <span v-if="error" :id="id ? `${id}-error` : undefined" class="base-input__error">
      {{ error }}
    </span>
  </label>
</template>

<style scoped>
.base-input {
  display: grid;
  gap: 6px;
  color: var(--color-text);
}

.base-input__label {
  font-size: 0.875rem;
  font-weight: 650;
}

.base-input__control {
  width: 100%;
  min-height: 42px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  padding: 0 12px;
  outline: none;
  transition:
    border-color 140ms ease,
    box-shadow 140ms ease;
}

.base-input__control::placeholder {
  color: var(--color-text-muted);
}

.base-input__control:focus {
  border-color: var(--color-focus);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-focus) 18%, transparent);
}

.base-input__control--invalid {
  border-color: var(--color-danger);
}

.base-input__error {
  color: var(--color-danger);
  font-size: 0.8125rem;
}
</style>
