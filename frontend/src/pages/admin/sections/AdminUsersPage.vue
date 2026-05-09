<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AdminUserCreateForm from '@/features/admin-rbac/ui/AdminUserCreateForm.vue'
import AdminUsersTable from '@/features/admin-rbac/ui/AdminUsersTable.vue'
import RoleAssignmentPanel from '@/features/admin-rbac/ui/RoleAssignmentPanel.vue'
import { useAdminRbacStore } from '@/features/admin-rbac/model/adminRbac.store'
import { useAuthStore } from '@/features/auth/model/auth.store'
import type { AdminUserRequestWritable } from '@/shared/api/generated/types.gen'
import { hasPermission } from '@/shared/lib/permissions/hasPermission'
import { PermissionCodes } from '@/shared/lib/permissions/permissionCodes'

const rbacStore = useAdminRbacStore()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.roleCodes.includes('ADMIN'))
const canCreateUser = computed(() => isAdmin.value || hasPermission(authStore.permissions, [PermissionCodes.USER_CREATE]))
const canUpdateUser = computed(() => isAdmin.value || hasPermission(authStore.permissions, [PermissionCodes.USER_UPDATE]))
const canDeleteUser = computed(() => isAdmin.value || hasPermission(authStore.permissions, [PermissionCodes.USER_DELETE]))
const canReadRoles = computed(() => isAdmin.value || hasPermission(authStore.permissions, [PermissionCodes.ROLE_READ]))

onMounted(() => {
  void loadPageData()
})

async function createUser(request: AdminUserRequestWritable): Promise<void> {
  await rbacStore.createUser(request)
}

async function loadPageData(): Promise<void> {
  await rbacStore.loadUsers()
  if (canReadRoles.value) {
    await rbacStore.loadRoles()
  }
}
</script>

<template>
  <main class="admin-users-page">
    <BaseCard v-if="canCreateUser" title="Create user" description="Create an administrative or client user.">
      <AdminUserCreateForm
        :loading="rbacStore.isSaving"
        :error-message="rbacStore.actionError?.message"
        @submit="createUser"
      />
    </BaseCard>

    <AppErrorMessage v-if="rbacStore.actionError" title="Action failed" :message="rbacStore.actionError.message" />
    <AppLoader v-if="rbacStore.isLoading" label="Loading users..." />
    <AppErrorMessage v-else-if="rbacStore.error" title="Could not load users" :message="rbacStore.error.message" />

    <div v-else class="admin-users-page__grid">
      <AdminUsersTable
        :users="rbacStore.users"
        :selected-user-id="rbacStore.selectedUserId"
        :current-user-id="authStore.user?.id"
        :can-update="canUpdateUser"
        :can-delete="canDeleteUser"
        :loading="rbacStore.isSaving"
        @select-user="rbacStore.selectUser"
        @update-user="rbacStore.updateUser"
        @delete-user="rbacStore.deleteUser"
      />
      <RoleAssignmentPanel
        v-if="canUpdateUser && canReadRoles"
        :selected-user-id="rbacStore.selectedUserId"
        :user-roles="rbacStore.selectedUserRoles"
        :roles="rbacStore.roles"
        :current-user-id="authStore.user?.id"
        :loading="rbacStore.isSaving"
        @assign-role="rbacStore.assignRoleToSelectedUser"
        @remove-role="rbacStore.removeRoleFromSelectedUser"
      />
    </div>
  </main>
</template>

<style scoped>
.admin-users-page,
.admin-users-page__grid {
  display: grid;
  gap: 20px;
}
</style>
