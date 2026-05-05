<script setup lang="ts">
import { reactive, ref } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { RoleRequest } from '@/shared/api/generated/types.gen'

withDefaults(
  defineProps<{ loading?: boolean; errorMessage?: string }>(),
  { loading: false, errorMessage: undefined },
)

const emit = defineEmits<{ submit: [value: RoleRequest] }>()

const form = reactive({ code: '', name: '', description: '' })
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

function submit(): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return

  emit('submit', {
    code: form.code.trim(),
    name: form.name.trim(),
    description: form.description.trim() || undefined,
  })
}

function validate(): Partial<Record<keyof typeof form, string>> {
  const errors: Partial<Record<keyof typeof form, string>> = {}
  if (!form.code.trim()) errors.code = 'Enter role code.'
  if (!form.name.trim()) errors.name = 'Enter role name.'
  return errors
}
</script>

<template>
  <form class="admin-role-create-form" novalidate @submit.prevent="submit">
    <AppErrorMessage :message="errorMessage" title="Could not create role" />
    <BaseInput id="admin-role-code" v-model="form.code" label="Code" :error="validationErrors.code" :disabled="loading" />
    <BaseInput id="admin-role-name" v-model="form.name" label="Name" :error="validationErrors.name" :disabled="loading" />
    <BaseInput id="admin-role-description" v-model="form.description" label="Description" :disabled="loading" />
    <BaseButton type="submit" :loading="loading">Create role</BaseButton>
  </form>
</template>

<style scoped>
.admin-role-create-form {
  display: grid;
  gap: 14px;
}
</style>
