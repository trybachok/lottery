<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'

defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <main class="admin-section-placeholder">
    <BaseCard :title="title" :description="description" />
  </main>
</template>

<style scoped>
.admin-section-placeholder {
  display: grid;
  gap: 20px;
}
</style>
