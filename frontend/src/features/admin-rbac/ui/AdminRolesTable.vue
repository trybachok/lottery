<script setup lang="ts">
import { reactive, ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Role, RoleRequest } from '@/shared/api/generated/types.gen'

withDefaults(
  defineProps<{ roles: Role[]; selectedRoleId?: string | null; canManage?: boolean; loading?: boolean }>(),
  { selectedRoleId: null, canManage: false, loading: false },
)
const emit = defineEmits<{
  selectRole: [roleId: string]
  updateRole: [roleId: string, value: RoleRequest]
  deleteRole: [roleId: string]
}>()

const columns = [
  { key: 'code', label: 'Code' },
  { key: 'name', label: 'Name' },
  { key: 'system', label: 'System' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof Role | 'actions'; label: string }>

const editingRoleId = ref<string | null>(null)
const form = reactive({ code: '', name: '', description: '' })
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

function edit(role: Role): void {
  editingRoleId.value = role.id
  form.code = role.code
  form.name = role.name
  form.description = role.description ?? ''
  validationErrors.value = {}
}

function cancel(): void {
  editingRoleId.value = null
  validationErrors.value = {}
}

function save(role: Role): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return

  emit('updateRole', role.id, {
    code: role.system ? role.code : form.code.trim(),
    name: form.name.trim(),
    description: form.description.trim() || undefined,
  })
  editingRoleId.value = null
}

function remove(role: Role): void {
  if (role.system) return
  if (!window.confirm(`Delete role ${role.code}?`)) return
  emit('deleteRole', role.id)
}

function validate(): Partial<Record<keyof typeof form, string>> {
  const errors: Partial<Record<keyof typeof form, string>> = {}
  if (!form.code.trim()) errors.code = 'Enter role code.'
  if (!form.name.trim()) errors.name = 'Enter role name.'
  return errors
}
</script>

<template>
  <BaseCard title="Roles" description="Select a role to manage permissions.">
    <BaseTable :columns="columns" :rows="roles" empty-message="No roles">
      <template #code="{ row }">
        <BaseInput
          v-if="editingRoleId === row.id"
          v-model="form.code"
          :error="validationErrors.code"
          :disabled="loading || row.system"
          aria-label="Code"
        />
        <span v-else>{{ row.code }}</span>
      </template>
      <template #name="{ row }">
        <BaseInput
          v-if="editingRoleId === row.id"
          v-model="form.name"
          :error="validationErrors.name"
          :disabled="loading"
          aria-label="Name"
        />
        <span v-else>{{ row.name }}</span>
      </template>
      <template #system="{ value }">{{ value ? 'Yes' : 'No' }}</template>
      <template #actions="{ row }">
        <div class="admin-roles-table__actions">
          <BaseButton size="sm" :variant="row.id === selectedRoleId ? 'primary' : 'secondary'" @click="$emit('selectRole', row.id)">
            Permissions
          </BaseButton>
          <template v-if="editingRoleId === row.id">
            <BaseInput v-model="form.description" placeholder="Description" :disabled="loading" aria-label="Description" />
            <BaseButton size="sm" :loading="loading" @click="save(row)">Save</BaseButton>
            <BaseButton size="sm" variant="ghost" :disabled="loading" @click="cancel">Cancel</BaseButton>
          </template>
          <template v-else-if="canManage">
            <BaseButton size="sm" variant="secondary" :disabled="loading" @click="edit(row)">Edit</BaseButton>
            <BaseButton size="sm" variant="danger" :disabled="loading || row.system" @click="remove(row)">Delete</BaseButton>
          </template>
        </div>
      </template>
    </BaseTable>
  </BaseCard>
</template>

<style scoped>
.admin-roles-table__actions {
  display: flex;
  min-width: 520px;
  flex-wrap: wrap;
  align-items: start;
  gap: 8px;
}
</style>
