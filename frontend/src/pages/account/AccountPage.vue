<script setup lang="ts">
import { useRouter } from 'vue-router'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'

const authStore = useAuthStore()
const router = useRouter()

async function logout(): Promise<void> {
  authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <main class="account-page">
    <BaseCard title="Account" description="Your lottery profile and tickets will appear here.">
      <dl class="account-page__details">
        <div>
          <dt>Email</dt>
          <dd>{{ authStore.user?.email }}</dd>
        </div>
        <div>
          <dt>Login</dt>
          <dd>{{ authStore.user?.login }}</dd>
        </div>
      </dl>

      <template #actions>
        <BaseButton variant="secondary" size="sm" @click="logout">Logout</BaseButton>
      </template>
    </BaseCard>
  </main>
</template>

<style scoped>
.account-page {
  width: min(100%, 960px);
  margin: 0 auto;
  padding: 32px 20px;
}

.account-page__details {
  display: grid;
  gap: 12px;
  margin: 0;
}

.account-page__details div {
  display: grid;
  gap: 4px;
}

dt {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 700;
}

dd {
  margin: 0;
}
</style>
