<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
</script>

<template>
  <main class="admin-dashboard">
    <BaseCard title="Dashboard" description="Operational overview will appear here as admin modules are implemented." />
  </main>
</template>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: 20px;
}
</style>
