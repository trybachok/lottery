<script setup lang="ts">
import { useRouter } from 'vue-router'
import BaseCard from '@/shared/ui/BaseCard.vue'
import RegisterForm from '@/features/auth/ui/RegisterForm.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import type { RegisterCredentials } from '@/features/auth/api/auth.api'

const authStore = useAuthStore()
const router = useRouter()

async function register(credentials: RegisterCredentials): Promise<void> {
  try {
    await authStore.register(credentials)
    await router.push('/account')
  } catch {
    // The store exposes the normalized API error to the form.
  }
}
</script>

<template>
  <main class="auth-page">
    <BaseCard title="Create account" description="Register and sign in to buy lottery tickets.">
      <RegisterForm
        :loading="authStore.isLoading"
        :error-message="authStore.error?.message"
        @submit="register"
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
