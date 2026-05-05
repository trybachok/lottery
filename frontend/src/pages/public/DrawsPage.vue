<script setup lang="ts">
import { onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import DrawList from '@/features/draws/ui/DrawList.vue'
import { useDrawsStore } from '@/features/draws/model/draws.store'

const drawsStore = useDrawsStore()

onMounted(() => {
  void drawsStore.loadDraws()
})
</script>

<template>
  <main class="draws-page">
    <AppLoader v-if="drawsStore.isLoading" label="Loading draws..." />
    <AppErrorMessage
      v-else-if="drawsStore.error"
      title="Could not load draws"
      :message="drawsStore.error.message"
    />
    <DrawList v-else :draws="drawsStore.items" />
  </main>
</template>

<style scoped>
.draws-page {
  width: min(100%, 960px);
  margin: 0 auto;
  padding: 32px 20px;
}
</style>
