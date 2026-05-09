<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import DrawDetailsCard from '@/features/draws/ui/DrawDetailsCard.vue'
import DrawResultCard from '@/features/draws/ui/DrawResultCard.vue'
import { useDrawsStore } from '@/features/draws/model/draws.store'

const route = useRoute()
const drawsStore = useDrawsStore()
const drawId = computed(() => String(route.params.drawId ?? ''))

watch(
  drawId,
  (value) => {
    if (value) {
      void drawsStore.loadDrawDetails(value)
    }
  },
  { immediate: true },
)
</script>

<template>
  <main class="draw-details-page">
    <AppLoader v-if="drawsStore.isLoadingDetails" label="Loading draw..." />
    <AppErrorMessage
      v-else-if="drawsStore.detailsError"
      title="Could not load draw"
      :message="drawsStore.detailsError.message"
    />

    <template v-else-if="drawsStore.selectedDraw">
      <DrawDetailsCard :draw="drawsStore.selectedDraw" />
      <DrawResultCard :result="drawsStore.selectedResult" :loading="drawsStore.isLoadingResult" />
      <AppErrorMessage
        v-if="drawsStore.resultError"
        title="Could not load result"
        :message="drawsStore.resultError.message"
      />

      <div class="draw-details-page__actions">
        <RouterLink :to="{ path: '/account', query: { drawId } }" custom v-slot="{ navigate }">
          <BaseButton @click="navigate">Create ticket</BaseButton>
        </RouterLink>
        <RouterLink to="/draws" custom v-slot="{ navigate }">
          <BaseButton variant="secondary" @click="navigate">Back to draws</BaseButton>
        </RouterLink>
      </div>
    </template>
  </main>
</template>

<style scoped>
.draw-details-page {
  display: grid;
  gap: 20px;
  width: min(100%, 960px);
  margin: 0 auto;
  padding: 32px 20px;
}

.draw-details-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
