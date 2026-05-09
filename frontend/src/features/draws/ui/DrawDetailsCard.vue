<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
import type { Draw } from '@/shared/api/generated/types.gen'

defineProps<{ draw: Draw }>()

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard :title="draw.title" :description="draw.description || 'Lottery draw details.'">
    <dl class="draw-details-card__grid">
      <div>
        <dt>Status</dt>
        <dd>{{ draw.status }}</dd>
      </div>
      <div>
        <dt>Sales</dt>
        <dd>{{ formatDate(draw.salesStartAt) }} - {{ formatDate(draw.salesEndAt) }}</dd>
      </div>
      <div>
        <dt>Draw time</dt>
        <dd>{{ formatDate(draw.drawAt) }}</dd>
      </div>
      <div>
        <dt>Max tickets</dt>
        <dd>{{ draw.maxTickets ?? 'No limit' }}</dd>
      </div>
      <div>
        <dt>Combination schema</dt>
        <dd>{{ draw.combinationSchemaId }}</dd>
      </div>
      <div>
        <dt>Version</dt>
        <dd>{{ draw.version }}</dd>
      </div>
    </dl>
  </BaseCard>
</template>

<style scoped>
.draw-details-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}

dt {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 700;
}

dd {
  margin: 4px 0 0;
  overflow-wrap: anywhere;
}

@media (max-width: 640px) {
  .draw-details-card__grid {
    grid-template-columns: 1fr;
  }
}
</style>
