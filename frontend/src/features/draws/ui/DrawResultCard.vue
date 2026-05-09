<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
import type { DrawResult } from '@/shared/api/generated/types.gen'

defineProps<{ result: DrawResult | null; loading?: boolean }>()

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard title="Draw result" description="Winning combination and audit proof for this draw.">
    <p v-if="loading" class="draw-result-card__muted">Loading result...</p>
    <p v-else-if="!result" class="draw-result-card__muted">Result is not published yet.</p>
    <dl v-else class="draw-result-card__grid">
      <div>
        <dt>Winning combination</dt>
        <dd>{{ result.winningCombinationValues.join(', ') }}</dd>
      </div>
      <div>
        <dt>Generated at</dt>
        <dd>{{ formatDate(result.generatedAt) }}</dd>
      </div>
      <div>
        <dt>Algorithm</dt>
        <dd>{{ result.algorithmVersion }}</dd>
      </div>
      <div>
        <dt>Random provider</dt>
        <dd>{{ result.randomProvider }}</dd>
      </div>
      <div v-if="result.proofHash">
        <dt>Proof hash</dt>
        <dd>{{ result.proofHash }}</dd>
      </div>
      <div v-if="result.requestId">
        <dt>Request ID</dt>
        <dd>{{ result.requestId }}</dd>
      </div>
    </dl>
  </BaseCard>
</template>

<style scoped>
.draw-result-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}

.draw-result-card__muted,
dt {
  color: var(--color-text-muted);
}

.draw-result-card__muted {
  margin: 0;
}

dt {
  font-size: 0.8125rem;
  font-weight: 700;
}

dd {
  margin: 4px 0 0;
  overflow-wrap: anywhere;
}

@media (max-width: 640px) {
  .draw-result-card__grid {
    grid-template-columns: 1fr;
  }
}
</style>
