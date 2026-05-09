<script setup lang="ts">
import { reactive, ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import { SystemPermissionCodes } from '@/shared/lib/permissions/permissionCodes'
import type { Permission, PermissionRequest } from '@/shared/api/generated/types.gen'

withDefaults(
  defineProps<{ permissions: Permission[]; canManage?: boolean; loading?: boolean }>(),
  { canManage: false, loading: false },
)
const emit = defineEmits<{
  updatePermission: [permissionId: string, value: PermissionRequest]
  deletePermission: [permissionId: string]
}>()

const columns = [
  { key: 'code', label: 'Code' },
  { key: 'description', label: 'Description' },
  { key: 'system', label: 'System' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof Permission | 'system' | 'actions'; label: string }>

const editingPermissionId = ref<string | null>(null)
const form = reactive({ code: '', description: '' })
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

function isSystemPermission(permission: Permission): boolean {
  return (SystemPermissionCodes as readonly string[]).includes(permission.code)
}

function edit(permission: Permission): void {
  editingPermissionId.value = permission.id
  form.code = permission.code
  form.description = permission.description ?? ''
  validationErrors.value = {}
}

function cancel(): void {
  editingPermissionId.value = null
  validationErrors.value = {}
}

function save(permission: Permission): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return

  emit('updatePermission', permission.id, {
    code: isSystemPermission(permission) ? permission.code : form.code.trim(),
    description: form.description.trim() || undefined,
  })
  editingPermissionId.value = null
}

function remove(permission: Permission): void {
  if (isSystemPermission(permission)) return
  if (!window.confirm(`Delete permission ${permission.code}?`)) return
  emit('deletePermission', permission.id)
}

function validate(): Partial<Record<keyof typeof form, string>> {
  return form.code.trim() ? {} : { code: 'Enter permission code.' }
}
</script>

<template>
  <BaseCard title="Permissions" description="System permissions available for RBAC.">
    <BaseTable :columns="columns" :rows="permissions" empty-message="No permissions">
      <template #code="{ row }">
        <BaseInput
          v-if="editingPermissionId === row.id"
          v-model="form.code"
          :error="validationErrors.code"
          :disabled="loading || isSystemPermission(row)"
          aria-label="Code"
        />
        <span v-else>{{ row.code }}</span>
      </template>
      <template #description="{ row }">
        <BaseInput
          v-if="editingPermissionId === row.id"
          v-model="form.description"
          :disabled="loading"
          aria-label="Description"
        />
        <span v-else>{{ row.description ?? '-' }}</span>
      </template>
      <template #system="{ row }">{{ isSystemPermission(row) ? 'Yes' : 'No' }}</template>
      <template #actions="{ row }">
        <div v-if="canManage" class="admin-permissions-table__actions">
          <template v-if="editingPermissionId === row.id">
            <BaseButton size="sm" :loading="loading" @click="save(row)">Save</BaseButton>
            <BaseButton size="sm" variant="ghost" :disabled="loading" @click="cancel">Cancel</BaseButton>
          </template>
          <template v-else>
            <BaseButton size="sm" variant="secondary" :disabled="loading" @click="edit(row)">Edit</BaseButton>
            <BaseButton size="sm" variant="danger" :disabled="loading || isSystemPermission(row)" @click="remove(row)">
              Delete
            </BaseButton>
          </template>
        </div>
      </template>
    </BaseTable>
  </BaseCard>
</template>

<style scoped>
.admin-permissions-table__actions {
  display: flex;
  min-width: 180px;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
