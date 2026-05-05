<script setup lang="ts">
import { reactive, ref } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { PermissionRequest } from '@/shared/api/generated/types.gen'

withDefaults(
  defineProps<{ loading?: boolean; errorMessage?: string }>(),
  { loading: false, errorMessage: undefined },
)

const emit = defineEmits<{ submit: [value: PermissionRequest] }>()

const form = reactive({ code: '', description: '' })
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

function submit(): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return

  emit('submit', {
    code: form.code.trim(),
    description: form.description.trim() || undefined,
  })
}

function validate(): Partial<Record<keyof typeof form, string>> {
  return form.code.trim() ? {} : { code: 'Enter permission code.' }
}
</script>

<template>
  <form class="admin-permission-create-form" novalidate @submit.prevent="submit">
    <AppErrorMessage :message="errorMessage" title="Could not create permission" />
    <BaseInput id="admin-permission-code" v-model="form.code" label="Code" :error="validationErrors.code" :disabled="loading" />
    <BaseInput id="admin-permission-description" v-model="form.description" label="Description" :disabled="loading" />
    <BaseButton type="submit" :loading="loading">Create permission</BaseButton>
  </form>
</template>

<style scoped>
.admin-permission-create-form {
  display: grid;
  gap: 14px;
}
</style>
