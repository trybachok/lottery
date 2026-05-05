<script setup lang="ts">
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Permission } from '@/shared/api/generated/types.gen'

defineProps<{ permissions: Permission[] }>()

const columns = [
  { key: 'code', label: 'Code' },
  { key: 'description', label: 'Description' },
] satisfies Array<{ key: keyof Permission; label: string }>
</script>

<template>
  <BaseCard title="Permissions" description="System permissions available for RBAC.">
    <BaseTable :columns="columns" :rows="permissions" empty-message="No permissions" />
  </BaseCard>
</template>
