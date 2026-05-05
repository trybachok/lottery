<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
</script>

<template>
  <main class="draws-page">
    <BaseCard title="Draws" description="Available lottery draws will appear here." />
  </main>
</template>

<style scoped>
.draws-page {
  width: min(100%, 960px);
  margin: 0 auto;
  padding: 32px 20px;
}
</style>
