<script setup lang="ts">
import { reactive, ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import BaseSelect from '@/shared/ui/BaseSelect.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { AdminUserRequestWritable, User, UserStatus } from '@/shared/api/generated/types.gen'

const props = withDefaults(
  defineProps<{
    users: User[]
    selectedUserId?: string | null
    currentUserId?: string | null
    canUpdate?: boolean
    canDelete?: boolean
    loading?: boolean
  }>(),
  {
    selectedUserId: null,
    currentUserId: null,
    canUpdate: false,
    canDelete: false,
    loading: false,
  },
)

const emit = defineEmits<{
  selectUser: [userId: string]
  updateUser: [userId: string, value: AdminUserRequestWritable]
  deleteUser: [userId: string]
}>()

const columns = [
  { key: 'email', label: 'Email' },
  { key: 'login', label: 'Login' },
  { key: 'status', label: 'Status' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof User | 'actions'; label: string }>

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Blocked', value: 'BLOCKED' },
  { label: 'Deleted', value: 'DELETED' },
]

const editingUserId = ref<string | null>(null)
const form = reactive({
  email: '',
  login: '',
  password: '',
  status: 'ACTIVE' as UserStatus,
})
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

function edit(user: User): void {
  editingUserId.value = user.id
  form.email = user.email
  form.login = user.login
  form.password = ''
  form.status = user.status
  validationErrors.value = {}
}

function cancel(): void {
  editingUserId.value = null
  validationErrors.value = {}
}

function save(userId: string): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return

  emit('updateUser', userId, {
    email: form.email.trim(),
    login: form.login.trim(),
    password: form.password || undefined,
    status: form.status,
  })
  editingUserId.value = null
}

function remove(user: User): void {
  if (user.id === props.currentUserId) return
  if (!window.confirm(`Delete user ${user.login}?`)) return
  emit('deleteUser', user.id)
}

function validate(): Partial<Record<keyof typeof form, string>> {
  const errors: Partial<Record<keyof typeof form, string>> = {}
  if (!form.email.trim() || !form.email.includes('@')) errors.email = 'Enter a valid email.'
  if (!form.login.trim()) errors.login = 'Enter login.'
  return errors
}
</script>

<template>
  <BaseCard title="Users" description="Select a user to manage roles.">
    <BaseTable :columns="columns" :rows="users" empty-message="No users">
      <template #email="{ row }">
        <BaseInput
          v-if="editingUserId === row.id"
          v-model="form.email"
          :error="validationErrors.email"
          :disabled="loading"
          aria-label="Email"
        />
        <span v-else>{{ row.email }}</span>
      </template>
      <template #login="{ row }">
        <BaseInput
          v-if="editingUserId === row.id"
          v-model="form.login"
          :error="validationErrors.login"
          :disabled="loading"
          aria-label="Login"
        />
        <span v-else>{{ row.login }}</span>
      </template>
      <template #status="{ row }">
        <BaseSelect
          v-if="editingUserId === row.id"
          v-model="form.status"
          :options="statusOptions"
          :disabled="loading"
          aria-label="Status"
        />
        <span v-else>{{ row.status }}</span>
      </template>
      <template #actions="{ row }">
        <div class="admin-users-table__actions">
          <BaseButton size="sm" :variant="row.id === selectedUserId ? 'primary' : 'secondary'" @click="$emit('selectUser', row.id)">
            Roles
          </BaseButton>
          <template v-if="editingUserId === row.id">
            <BaseInput v-model="form.password" type="password" placeholder="New password" :disabled="loading" aria-label="Password" />
            <BaseButton size="sm" :loading="loading" @click="save(row.id)">Save</BaseButton>
            <BaseButton size="sm" variant="ghost" :disabled="loading" @click="cancel">Cancel</BaseButton>
          </template>
          <template v-else>
            <BaseButton v-if="canUpdate" size="sm" variant="secondary" :disabled="loading" @click="edit(row)">Edit</BaseButton>
            <BaseButton
              v-if="canDelete"
              size="sm"
              variant="danger"
              :disabled="loading || row.id === currentUserId"
              @click="remove(row)"
            >
              Delete
            </BaseButton>
          </template>
        </div>
      </template>
    </BaseTable>
  </BaseCard>
</template>

<style scoped>
.admin-users-table__actions {
  display: flex;
  min-width: 520px;
  flex-wrap: wrap;
  align-items: start;
  gap: 8px;
}
</style>
