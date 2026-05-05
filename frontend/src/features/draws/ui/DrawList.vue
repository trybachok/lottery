<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Draw } from '@/shared/api/generated/types.gen'

defineProps<{
  draws: Draw[]
}>()

const columns = [
  { key: 'title', label: 'Draw' },
  { key: 'status', label: 'Status' },
  { key: 'salesEndAt', label: 'Sales end' },
  { key: 'drawAt', label: 'Draw time' },
  { key: 'maxTickets', label: 'Max tickets', align: 'right' },
] satisfies Array<{ key: keyof Draw & string; label: string; align?: 'left' | 'right' | 'center' }>

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard title="Draws" description="Available lottery draws.">
    <BaseTable :columns="columns" :rows="draws" empty-message="No draws available">
      <template #salesEndAt="{ value }">
        {{ formatDate(String(value)) }}
      </template>
      <template #drawAt="{ value }">
        {{ formatDate(String(value)) }}
      </template>
      <template #maxTickets="{ value }">
        {{ value ?? '-' }}
      </template>
    </BaseTable>
  </BaseCard>
</template>
