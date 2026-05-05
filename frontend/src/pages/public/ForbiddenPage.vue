<script setup lang="ts">
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
</script>

<template>
  <main class="forbidden-page">
    <BaseCard title="Access denied" description="This page requires additional permissions.">
      <AppErrorMessage
        title="Forbidden"
        message="Your account is signed in, but it does not have enough permissions for this page."
      />

      <template #actions>
        <a href="/draws">
          <BaseButton size="sm">Go to draws</BaseButton>
        </a>
      </template>
    </BaseCard>
  </main>
</template>

<style scoped>
.forbidden-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
}

.forbidden-page :deep(.base-card) {
  width: min(100%, 520px);
}

a {
  text-decoration: none;
}
</style>
