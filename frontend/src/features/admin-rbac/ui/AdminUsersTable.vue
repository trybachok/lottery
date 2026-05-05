<script setup lang="ts">
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { User } from '@/shared/api/generated/types.gen'

defineProps<{ users: User[]; selectedUserId?: string | null }>()
defineEmits<{ selectUser: [userId: string] }>()

const columns = [
  { key: 'email', label: 'Email' },
  { key: 'login', label: 'Login' },
  { key: 'status', label: 'Status' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof User | 'actions'; label: string }>
</script>

<template>
  <BaseCard title="Users" description="Select a user to manage roles.">
    <BaseTable :columns="columns" :rows="users" empty-message="No users">
      <template #actions="{ row }">
        <BaseButton size="sm" :variant="row.id === selectedUserId ? 'primary' : 'secondary'" @click="$emit('selectUser', row.id)">
          Roles
        </BaseButton>
      </template>
    </BaseTable>
  </BaseCard>
</template>
