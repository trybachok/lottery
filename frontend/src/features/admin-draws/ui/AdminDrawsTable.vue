<script setup lang="ts">
import { reactive } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Draw } from '@/shared/api/generated/types.gen'

const props = defineProps<{
  draws: Draw[]
  generatingDrawId?: string | null
  runningDrawId?: string | null
  assigningManagerDrawId?: string | null
  canGenerate?: boolean
  canRun?: boolean
  canAssignManager?: boolean
}>()

const emit = defineEmits<{
  generateWinningCombination: [drawId: string]
  runDraw: [drawId: string]
  assignManager: [drawId: string, managerId: string]
}>()

const managerIds = reactive<Record<string, string>>({})

const columns = [
  { key: 'title', label: 'Draw' },
  { key: 'status', label: 'Status' },
  { key: 'salesEndAt', label: 'Sales end' },
  { key: 'drawAt', label: 'Draw time' },
  { key: 'manager', label: 'Manager' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof Draw | 'manager' | 'actions'; label: string }>

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function assignManager(drawId: string): void {
  const managerId = managerIds[drawId]?.trim()

  if (!managerId) {
    return
  }

  emit('assignManager', drawId, managerId)
}

function canRunByStatus(draw: Draw): boolean {
  return draw.status === 'SALES_CLOSED' || draw.status === 'DRAWING'
}

function canGenerateByStatus(draw: Draw): boolean {
  return draw.status === 'SALES_CLOSED'
}
</script>

<template>
  <BaseCard title="Draw management" description="Create, assign and run lottery draws.">
    <BaseTable :columns="columns" :rows="props.draws" empty-message="No draws">
      <template #salesEndAt="{ value }">
        {{ formatDate(String(value)) }}
      </template>

      <template #drawAt="{ value }">
        {{ formatDate(String(value)) }}
      </template>

      <template #manager="{ row }">
        <div v-if="canAssignManager" class="admin-draws-table__manager">
          <BaseInput
            :id="`manager-${row.id}`"
            v-model="managerIds[row.id]"
            placeholder="Manager UUID"
            :disabled="assigningManagerDrawId === row.id"
          />
          <BaseButton
            size="sm"
            variant="secondary"
            :loading="assigningManagerDrawId === row.id"
            @click="assignManager(row.id)"
          >
            Assign
          </BaseButton>
        </div>
        <span v-else class="admin-draws-table__muted">{{ row.managerId ?? '-' }}</span>
      </template>

      <template #actions="{ row }">
        <div v-if="props.canGenerate || props.canRun" class="admin-draws-table__actions">
          <BaseButton
            v-if="props.canGenerate"
            size="sm"
            variant="secondary"
            :disabled="!canGenerateByStatus(row)"
            :loading="generatingDrawId === row.id"
            @click="$emit('generateWinningCombination', row.id)"
          >
            Generate combination
          </BaseButton>
          <BaseButton
            v-if="props.canRun"
            size="sm"
            :disabled="!canRunByStatus(row)"
            :loading="runningDrawId === row.id"
            @click="$emit('runDraw', row.id)"
          >
            Run
          </BaseButton>
        </div>
        <span v-else class="admin-draws-table__muted">No actions</span>
      </template>
    </BaseTable>
  </BaseCard>
</template>

<style scoped>
.admin-draws-table__manager {
  display: grid;
  min-width: 260px;
  grid-template-columns: minmax(160px, 1fr) auto;
  gap: 8px;
}

.admin-draws-table__actions {
  display: flex;
  min-width: 260px;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-draws-table__muted {
  color: var(--color-text-muted);
}
</style>
