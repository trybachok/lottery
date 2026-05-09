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
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: (keyof Draw & string) | 'actions'; label: string; align?: 'left' | 'right' | 'center' }>

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
      <template #actions="{ row }">
        <RouterLink class="draw-list__link" :to="`/draws/${row.id}`">Details</RouterLink>
      </template>
    </BaseTable>
  </BaseCard>
</template>

<style scoped>
.draw-list__link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}

.draw-list__link:hover {
  text-decoration: underline;
}
</style>
