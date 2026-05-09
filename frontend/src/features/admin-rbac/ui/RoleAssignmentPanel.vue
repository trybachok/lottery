<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseSelect from '@/shared/ui/BaseSelect.vue'
import type { Role } from '@/shared/api/generated/types.gen'

const props = withDefaults(
  defineProps<{
    selectedUserId?: string | null
    userRoles: Role[]
    roles: Role[]
    currentUserId?: string | null
    loading?: boolean
  }>(),
  { selectedUserId: null, currentUserId: null, loading: false },
)
const emit = defineEmits<{ assignRole: [roleId: string]; removeRole: [roleId: string] }>()
const roleId = ref('')

const availableRoleOptions = computed(() => {
  const assigned = new Set(props.userRoles.map((role) => role.id))
  return [
    { label: 'Select role', value: '' },
    ...props.roles
      .filter((role) => !assigned.has(role.id))
      .map((role) => ({ label: `${role.code} - ${role.name}`, value: role.id })),
  ]
})

function assign(): void {
  if (!roleId.value.trim()) return
  emit('assignRole', roleId.value.trim())
  roleId.value = ''
}

function remove(role: Role): void {
  if (props.selectedUserId === props.currentUserId && role.code === 'ADMIN') return
  emit('removeRole', role.id)
}
</script>

<template>
  <BaseCard title="User roles" :description="selectedUserId ? 'Assign roles to the selected user.' : 'Select a user first.'">
    <div class="assignment-panel">
      <div class="assignment-panel__assign">
        <BaseSelect
          id="assign-user-role"
          v-model="roleId"
          label="Role"
          :options="availableRoleOptions"
          :disabled="!selectedUserId || loading"
        />
        <BaseButton :disabled="!selectedUserId || !roleId" :loading="loading" @click="assign">Assign</BaseButton>
      </div>

      <ul class="assignment-panel__list">
        <li v-for="role in userRoles" :key="role.id" class="assignment-panel__item">
          <span>{{ role.code }}</span>
          <BaseButton
            size="sm"
            variant="ghost"
            :disabled="loading || (selectedUserId === currentUserId && role.code === 'ADMIN')"
            @click="remove(role)"
          >
            Remove
          </BaseButton>
        </li>
        <li v-if="userRoles.length === 0" class="assignment-panel__empty">No roles</li>
      </ul>
    </div>
  </BaseCard>
</template>

<style scoped>
.assignment-panel {
  display: grid;
  gap: 12px;
}

.assignment-panel__assign,
.assignment-panel__item {
  display: flex;
  align-items: end;
  gap: 10px;
}

.assignment-panel__assign > :first-child {
  flex: 1;
}

.assignment-panel__list {
  margin: 0;
  padding: 0;
  color: var(--color-text-muted);
  list-style: none;
}

.assignment-panel__item {
  justify-content: space-between;
  border-top: 1px solid var(--color-border);
  padding: 10px 0;
}

.assignment-panel__empty {
  padding: 10px 0;
}
</style>
