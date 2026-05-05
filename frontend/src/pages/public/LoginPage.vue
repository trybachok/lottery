<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import BaseCard from '@/shared/ui/BaseCard.vue'
import LoginForm from '@/features/auth/ui/LoginForm.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import type { LoginCredentials } from '@/features/auth/api/auth.api'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

async function login(credentials: LoginCredentials): Promise<void> {
  try {
    await authStore.login(credentials)
    await router.push(resolveRedirectPath())
  } catch {
    // The store exposes the normalized API error to the form.
  }
}

function resolveRedirectPath(): string {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/account'
}
</script>

<template>
  <main class="auth-page">
    <BaseCard title="Sign in" description="Use your lottery account credentials.">
      <LoginForm
        :loading="authStore.isLoading"
        :error-message="authStore.error?.message"
        @submit="login"
      />
    </BaseCard>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
}

.auth-page :deep(.base-card) {
  width: min(100%, 420px);
}
</style>
