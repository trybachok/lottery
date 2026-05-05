<script setup lang="ts">
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Role } from '@/shared/api/generated/types.gen'

defineProps<{ roles: Role[]; selectedRoleId?: string | null }>()
defineEmits<{ selectRole: [roleId: string] }>()

const columns = [
  { key: 'code', label: 'Code' },
  { key: 'name', label: 'Name' },
  { key: 'system', label: 'System' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof Role | 'actions'; label: string }>
</script>

<template>
  <BaseCard title="Roles" description="Select a role to manage permissions.">
    <BaseTable :columns="columns" :rows="roles" empty-message="No roles">
      <template #system="{ value }">{{ value ? 'Yes' : 'No' }}</template>
      <template #actions="{ row }">
        <BaseButton size="sm" :variant="row.id === selectedRoleId ? 'primary' : 'secondary'" @click="$emit('selectRole', row.id)">
          Permissions
        </BaseButton>
      </template>
    </BaseTable>
  </BaseCard>
</template>
