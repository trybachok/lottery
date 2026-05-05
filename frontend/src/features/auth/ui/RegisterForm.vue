<script setup lang="ts">
import { reactive, ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import type { RegisterCredentials } from '../api/auth.api'

type RegisterFormState = {
  email: string
  login: string
  password: string
}

withDefaults(
  defineProps<{
    loading?: boolean
    errorMessage?: string
  }>(),
  {
    loading: false,
    errorMessage: undefined,
  },
)

const emit = defineEmits<{
  submit: [credentials: RegisterCredentials]
}>()

const form = reactive<RegisterFormState>({
  email: '',
  login: '',
  password: '',
})

const validationErrors = ref<Partial<Record<keyof RegisterCredentials, string>>>({})

function submit(): void {
  validationErrors.value = validate()

  if (Object.keys(validationErrors.value).length > 0) {
    return
  }

  emit('submit', {
    email: form.email.trim(),
    login: form.login?.trim() || undefined,
    password: form.password,
  })
}

function validate(): Partial<Record<keyof RegisterCredentials, string>> {
  const errors: Partial<Record<keyof RegisterCredentials, string>> = {}

  if (!form.email.trim()) {
    errors.email = 'Enter email.'
  }

  if (form.email && !form.email.includes('@')) {
    errors.email = 'Enter a valid email.'
  }

  if (!form.password) {
    errors.password = 'Enter password.'
  }

  if (form.password && form.password.length < 8) {
    errors.password = 'Password must contain at least 8 characters.'
  }

  return errors
}
</script>

<template>
  <form class="register-form" novalidate @submit.prevent="submit">
    <AppErrorMessage :message="errorMessage" title="Registration failed" />

    <BaseInput
      id="register-email"
      v-model="form.email"
      label="Email"
      type="email"
      autocomplete="email"
      placeholder="client@example.com"
      :error="validationErrors.email"
      :disabled="loading"
    />

    <BaseInput
      id="register-login"
      v-model="form.login"
      label="Login"
      autocomplete="username"
      placeholder="client"
      :disabled="loading"
    />

    <BaseInput
      id="register-password"
      v-model="form.password"
      label="Password"
      type="password"
      autocomplete="new-password"
      :error="validationErrors.password"
      :disabled="loading"
    />

    <BaseButton type="submit" :loading="loading">Create account</BaseButton>
  </form>
</template>

<style scoped>
.register-form {
  display: grid;
  gap: 16px;
}
</style>
