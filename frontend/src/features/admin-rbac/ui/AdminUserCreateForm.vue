<script setup lang="ts">
import { reactive, ref } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { AdminUserRequestWritable, UserStatus } from '@/shared/api/generated/types.gen'

withDefaults(
  defineProps<{ loading?: boolean; errorMessage?: string }>(),
  { loading: false, errorMessage: undefined },
)

const emit = defineEmits<{ submit: [value: AdminUserRequestWritable] }>()

const form = reactive({
  email: '',
  login: '',
  password: '',
  status: 'ACTIVE' as UserStatus,
})
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

function submit(): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return

  emit('submit', {
    email: form.email.trim(),
    login: form.login.trim(),
    password: form.password || undefined,
    status: form.status,
  })
}

function validate(): Partial<Record<keyof typeof form, string>> {
  const errors: Partial<Record<keyof typeof form, string>> = {}
  if (!form.email.trim() || !form.email.includes('@')) errors.email = 'Enter a valid email.'
  if (!form.login.trim()) errors.login = 'Enter login.'
  return errors
}
</script>

<template>
  <form class="admin-user-create-form" novalidate @submit.prevent="submit">
    <AppErrorMessage :message="errorMessage" title="Could not create user" />
    <BaseInput id="admin-user-email" v-model="form.email" label="Email" :error="validationErrors.email" :disabled="loading" />
    <BaseInput id="admin-user-login" v-model="form.login" label="Login" :error="validationErrors.login" :disabled="loading" />
    <BaseInput id="admin-user-password" v-model="form.password" label="Password" type="password" :disabled="loading" />
    <BaseButton type="submit" :loading="loading">Create user</BaseButton>
  </form>
</template>

<style scoped>
.admin-user-create-form {
  display: grid;
  gap: 14px;
}
</style>
