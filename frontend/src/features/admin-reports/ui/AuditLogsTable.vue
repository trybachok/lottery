<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { AuditLog } from '@/shared/api/generated/types.gen'

defineProps<{
  auditLogs: AuditLog[]
}>()

const columns = [
  { key: 'action', label: 'Action' },
  { key: 'entityType', label: 'Entity' },
  { key: 'entityId', label: 'Entity ID' },
  { key: 'actorRoleCodes', label: 'Actor roles' },
  { key: 'requestId', label: 'Request ID' },
  { key: 'createdAt', label: 'Created' },
] satisfies Array<{ key: keyof AuditLog; label: string }>

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard title="Audit logs" description="Administrative audit trail with backend-side authorization.">
    <BaseTable :columns="columns" :rows="auditLogs" empty-message="No audit logs match the selected filters">
      <template #entityId="{ value }">
        {{ value ?? '-' }}
      </template>

      <template #actorRoleCodes="{ value }">
        {{ Array.isArray(value) && value.length > 0 ? value.join(', ') : '-' }}
      </template>

      <template #createdAt="{ value }">
        {{ formatDate(String(value)) }}
      </template>
    </BaseTable>
  </BaseCard>
</template>
