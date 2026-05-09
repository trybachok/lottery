<script setup lang="ts">
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import type { Ticket } from '@/shared/api/generated/types.gen'

defineProps<{
  ticket: Ticket
  checking?: boolean
}>()

defineEmits<{
  checkResult: []
}>()

function formatDate(value?: string): string {
  if (!value) return '-'
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard title="Ticket details" description="Ticket status, combination and result information.">
    <template #actions>
      <BaseButton size="sm" variant="secondary" :loading="checking" @click="$emit('checkResult')">
        Check result
      </BaseButton>
    </template>

    <dl class="ticket-details-card__grid">
      <div>
        <dt>Status</dt>
        <dd>{{ ticket.status }}</dd>
      </div>
      <div>
        <dt>Draw</dt>
        <dd><RouterLink class="ticket-details-card__link" :to="`/draws/${ticket.drawId}`">{{ ticket.drawId }}</RouterLink></dd>
      </div>
      <div>
        <dt>Combination</dt>
        <dd>{{ ticket.combinationValues.join(', ') }}</dd>
      </div>
      <div>
        <dt>Price</dt>
        <dd>{{ ticket.priceAmount }} {{ ticket.priceCurrency }}</dd>
      </div>
      <div>
        <dt>Created</dt>
        <dd>{{ formatDate(ticket.createdAt) }}</dd>
      </div>
      <div>
        <dt>Participated</dt>
        <dd>{{ formatDate(ticket.participatedAt) }}</dd>
      </div>
      <div>
        <dt>Checked</dt>
        <dd>{{ formatDate(ticket.checkedAt) }}</dd>
      </div>
      <div>
        <dt>Match percent</dt>
        <dd>{{ ticket.matchPercent ?? '-' }}</dd>
      </div>
      <div>
        <dt>Prize</dt>
        <dd>{{ ticket.prizeId ?? '-' }}</dd>
      </div>
    </dl>
  </BaseCard>
</template>

<style scoped>
.ticket-details-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}

.ticket-details-card__link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}

.ticket-details-card__link:hover {
  text-decoration: underline;
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
  .ticket-details-card__grid {
    grid-template-columns: 1fr;
  }
}
</style>
