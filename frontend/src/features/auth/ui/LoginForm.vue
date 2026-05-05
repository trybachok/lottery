<script setup lang="ts">
import { reactive, ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import type { LoginCredentials } from '../api/auth.api'

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
  submit: [credentials: LoginCredentials]
}>()

const form = reactive<LoginCredentials>({
  loginOrEmail: '',
  password: '',
})

const validationErrors = ref<Partial<Record<keyof LoginCredentials, string>>>({})

function submit(): void {
  validationErrors.value = validate()

  if (Object.keys(validationErrors.value).length > 0) {
    return
  }

  emit('submit', {
    loginOrEmail: form.loginOrEmail.trim(),
    password: form.password,
  })
}

function validate(): Partial<Record<keyof LoginCredentials, string>> {
  const errors: Partial<Record<keyof LoginCredentials, string>> = {}

  if (!form.loginOrEmail.trim()) {
    errors.loginOrEmail = 'Enter login or email.'
  }

  if (!form.password) {
    errors.password = 'Enter password.'
  }

  return errors
}
</script>

<template>
  <form class="login-form" novalidate @submit.prevent="submit">
    <AppErrorMessage :message="errorMessage" title="Sign in failed" />

    <BaseInput
      id="login-email"
      v-model="form.loginOrEmail"
      label="Login or email"
      autocomplete="username"
      placeholder="client@example.com"
      :error="validationErrors.loginOrEmail"
      :disabled="loading"
    />

    <BaseInput
      id="login-password"
      v-model="form.password"
      label="Password"
      type="password"
      autocomplete="current-password"
      :error="validationErrors.password"
      :disabled="loading"
    />

    <BaseButton type="submit" :loading="loading">Sign in</BaseButton>
  </form>
</template>

<style scoped>
.login-form {
  display: grid;
  gap: 16px;
}
</style>
