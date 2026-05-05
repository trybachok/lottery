<script setup lang="ts">
export type BaseSelectOption = {
  label: string
  value: string
}

defineProps<{
  id?: string
  modelValue: string
  label?: string
  options: BaseSelectOption[]
  error?: string
  disabled?: boolean
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <label class="base-select">
    <span v-if="label" class="base-select__label">{{ label }}</span>
    <select
      :id="id"
      class="base-select__control"
      :class="{ 'base-select__control--invalid': error }"
      :value="modelValue"
      :disabled="disabled"
      :aria-invalid="Boolean(error)"
      :aria-describedby="error && id ? `${id}-error` : undefined"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option v-for="option in options" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
    <span v-if="error" :id="id ? `${id}-error` : undefined" class="base-select__error">
      {{ error }}
    </span>
  </label>
</template>

<style scoped>
.base-select {
  display: grid;
  gap: 6px;
  color: var(--color-text);
}

.base-select__label {
  font-size: 0.875rem;
  font-weight: 650;
}

.base-select__control {
  width: 100%;
  min-height: 42px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  padding: 0 36px 0 12px;
  outline: none;
  transition:
    border-color 140ms ease,
    box-shadow 140ms ease;
}

.base-select__control:focus {
  border-color: var(--color-focus);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-focus) 18%, transparent);
}

.base-select__control--invalid {
  border-color: var(--color-danger);
}

.base-select__error {
  color: var(--color-danger);
  font-size: 0.8125rem;
}
</style>
