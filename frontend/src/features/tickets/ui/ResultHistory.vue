<script setup lang="ts">
import { computed } from 'vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Ticket } from '@/shared/api/generated/types.gen'

const props = defineProps<{ tickets: Ticket[] }>()

const resultStatuses = new Set(['PARTICIPATED', 'NOT_PARTICIPATED', 'CHECKED', 'WIN', 'LOSE'])
const resultTickets = computed(() =>
  props.tickets.filter(
    (ticket) =>
      resultStatuses.has(ticket.status) ||
      Boolean(ticket.checkedAt) ||
      ticket.matchPercent !== undefined ||
      Boolean(ticket.prizeId),
  ),
)

const columns = [
  { key: 'drawId', label: 'Draw' },
  { key: 'status', label: 'Result' },
  { key: 'matchPercent', label: 'Match', align: 'right' },
  { key: 'checkedAt', label: 'Checked' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof Ticket | 'actions'; label: string; align?: 'left' | 'right' | 'center' }>

function formatDate(value?: string): string {
  if (!value) return '-'
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard title="Result history" description="Completed and checked tickets.">
    <BaseTable :columns="columns" :rows="resultTickets" empty-message="No checked results yet">
      <template #drawId="{ value }">
        <RouterLink class="result-history__link" :to="`/draws/${value}`">{{ value }}</RouterLink>
      </template>
      <template #matchPercent="{ value }">
        {{ value ?? '-' }}
      </template>
      <template #checkedAt="{ value }">
        {{ formatDate(value ? String(value) : undefined) }}
      </template>
      <template #actions="{ row }">
        <RouterLink class="result-history__link" :to="`/account/tickets/${row.id}`">Details</RouterLink>
      </template>
    </BaseTable>
  </BaseCard>
</template>

<style scoped>
.result-history__link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}

.result-history__link:hover {
  text-decoration: underline;
}
</style>
