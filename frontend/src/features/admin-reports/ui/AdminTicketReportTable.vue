<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Ticket } from '@/shared/api/generated/types.gen'

defineProps<{
  tickets: Ticket[]
}>()

const columns = [
  { key: 'drawId', label: 'Draw ID' },
  { key: 'userId', label: 'User ID' },
  { key: 'status', label: 'Status' },
  { key: 'combinationValues', label: 'Combination' },
  { key: 'priceAmount', label: 'Price', align: 'right' },
  { key: 'createdAt', label: 'Created' },
] satisfies Array<{ key: keyof Ticket; label: string; align?: 'left' | 'right' | 'center' }>

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard title="Ticket report" description="Filtered ticket report rows returned by the backend.">
    <BaseTable :columns="columns" :rows="tickets" empty-message="No tickets match the selected filters">
      <template #combinationValues="{ value }">
        {{ Array.isArray(value) ? value.join(', ') : '-' }}
      </template>

      <template #priceAmount="{ row }">
        {{ row.priceAmount }} {{ row.priceCurrency }}
      </template>

      <template #createdAt="{ value }">
        {{ formatDate(String(value)) }}
      </template>
    </BaseTable>
  </BaseCard>
</template>
