<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseSelect from '@/shared/ui/BaseSelect.vue'
import type { Permission, Role } from '@/shared/api/generated/types.gen'

const props = withDefaults(
  defineProps<{
    selectedRoleId?: string | null
    selectedRole?: Role | null
    rolePermissions: Permission[]
    permissions: Permission[]
    loading?: boolean
  }>(),
  { selectedRoleId: null, selectedRole: null, loading: false },
)
const emit = defineEmits<{ assignPermission: [permissionId: string]; removePermission: [permissionId: string] }>()
const permissionId = ref('')

const locked = computed(() => Boolean(props.selectedRole?.system))
const availablePermissionOptions = computed(() => {
  const assigned = new Set(props.rolePermissions.map((permission) => permission.id))
  return [
    { label: 'Select permission', value: '' },
    ...props.permissions
      .filter((permission) => !assigned.has(permission.id))
      .map((permission) => ({ label: permission.code, value: permission.id })),
  ]
})

function assign(): void {
  if (!permissionId.value.trim()) return
  emit('assignPermission', permissionId.value.trim())
  permissionId.value = ''
}
</script>

<template>
  <BaseCard title="Role permissions" :description="selectedRoleId ? 'Assign permissions to the selected role.' : 'Select a role first.'">
    <div class="assignment-panel">
      <div class="assignment-panel__assign">
        <BaseSelect
          id="assign-role-permission"
          v-model="permissionId"
          label="Permission"
          :options="availablePermissionOptions"
          :disabled="!selectedRoleId || locked || loading"
        />
        <BaseButton :disabled="!selectedRoleId || locked || !permissionId" :loading="loading" @click="assign">Assign</BaseButton>
      </div>

      <ul class="assignment-panel__list">
        <li v-for="permission in rolePermissions" :key="permission.id" class="assignment-panel__item">
          <span>{{ permission.code }}</span>
          <BaseButton size="sm" variant="ghost" :disabled="loading || locked" @click="$emit('removePermission', permission.id)">
            Remove
          </BaseButton>
        </li>
        <li v-if="rolePermissions.length === 0" class="assignment-panel__empty">No permissions</li>
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
