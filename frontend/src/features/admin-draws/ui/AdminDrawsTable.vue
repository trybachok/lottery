<script setup lang="ts">
import { reactive } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Draw } from '@/shared/api/generated/types.gen'

const props = defineProps<{
  draws: Draw[]
  activatingDrawId?: string | null
  closingSalesDrawId?: string | null
  generatingDrawId?: string | null
  runningDrawId?: string | null
  assigningManagerDrawId?: string | null
  currentUserId?: string
  isAdmin?: boolean
  isManager?: boolean
  canActivate?: boolean
  canCloseSales?: boolean
  canGenerate?: boolean
  canRun?: boolean
  canAssignManager?: boolean
}>()

const emit = defineEmits<{
  activateDraw: [drawId: string]
  closeSales: [drawId: string]
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

function canActivateByStatus(draw: Draw): boolean {
  return ['DRAFT', 'SCHEDULED', 'PAUSED', 'POSTPONED'].includes(draw.status)
}

function canCloseSalesByStatus(draw: Draw): boolean {
  return draw.status === 'ACTIVE'
}

function hasVisibleActions(draw: Draw): boolean {
  return canActivateByStatus(draw) || canCloseSalesByStatus(draw) || Boolean(props.canGenerate || props.canRun)
}

function canManageLifecycleByAccess(draw: Draw, allowed?: boolean): boolean {
  if (!allowed) {
    return false
  }
  if (props.isAdmin) {
    return true
  }
  return Boolean(props.isManager && props.currentUserId && draw.managerId === props.currentUserId)
}

function canActivateByAccess(draw: Draw): boolean {
  return canManageLifecycleByAccess(draw, props.canActivate)
}

function canCloseSalesByAccess(draw: Draw): boolean {
  return canManageLifecycleByAccess(draw, props.canCloseSales)
}

function activateDisabledReason(draw: Draw): string | undefined {
  if (!canActivateByStatus(draw)) {
    return 'Draw cannot be activated from this status.'
  }
  if (!canActivateByAccess(draw)) {
    return 'Only an administrator or the assigned draw manager can activate this draw.'
  }
  return undefined
}

function closeSalesDisabledReason(draw: Draw): string | undefined {
  if (!canCloseSalesByStatus(draw)) {
    return 'Sales can be closed only for ACTIVE draw.'
  }
  if (!canCloseSalesByAccess(draw)) {
    return 'Only an administrator or the assigned draw manager can close sales.'
  }
  return undefined
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
        <div v-if="hasVisibleActions(row)" class="admin-draws-table__actions">
          <BaseButton
            v-if="canActivateByStatus(row)"
            size="sm"
            variant="secondary"
            :disabled="!canActivateByAccess(row)"
            :loading="activatingDrawId === row.id"
            :title="activateDisabledReason(row)"
            @click="$emit('activateDraw', row.id)"
          >
            Activate
          </BaseButton>
          <BaseButton
            v-if="canCloseSalesByStatus(row)"
            size="sm"
            variant="secondary"
            :disabled="!canCloseSalesByAccess(row)"
            :loading="closingSalesDrawId === row.id"
            :title="closeSalesDisabledReason(row)"
            @click="$emit('closeSales', row.id)"
          >
            Close sales
          </BaseButton>
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
