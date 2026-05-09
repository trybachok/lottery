<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AdminRoleCreateForm from '@/features/admin-rbac/ui/AdminRoleCreateForm.vue'
import AdminRolesTable from '@/features/admin-rbac/ui/AdminRolesTable.vue'
import PermissionAssignmentPanel from '@/features/admin-rbac/ui/PermissionAssignmentPanel.vue'
import { useAdminRbacStore } from '@/features/admin-rbac/model/adminRbac.store'
import { useAuthStore } from '@/features/auth/model/auth.store'
import type { RoleRequest } from '@/shared/api/generated/types.gen'
import { hasPermission } from '@/shared/lib/permissions/hasPermission'
import { PermissionCodes } from '@/shared/lib/permissions/permissionCodes'

const rbacStore = useAdminRbacStore()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.roleCodes.includes('ADMIN'))
const canManageRoles = computed(
  () => isAdmin.value || hasPermission(authStore.permissions, [PermissionCodes.ROLE_MANAGE]),
)
const canReadPermissions = computed(
  () => isAdmin.value || hasPermission(authStore.permissions, [PermissionCodes.PERMISSION_MANAGE]),
)
const selectedRole = computed(() => rbacStore.roles.find((role) => role.id === rbacStore.selectedRoleId) ?? null)

onMounted(() => {
  void loadPageData()
})

async function createRole(request: RoleRequest): Promise<void> {
  await rbacStore.createRole(request)
}

async function loadPageData(): Promise<void> {
  await rbacStore.loadRoles()
  if (canReadPermissions.value) {
    await rbacStore.loadPermissions()
  }
}
</script>

<template>
  <main class="admin-roles-page">
    <BaseCard v-if="canManageRoles" title="Create role" description="Create a role and then assign permissions to it.">
      <AdminRoleCreateForm
        :loading="rbacStore.isSaving"
        :error-message="rbacStore.actionError?.message"
        @submit="createRole"
      />
    </BaseCard>

    <AppErrorMessage v-if="rbacStore.actionError" title="Action failed" :message="rbacStore.actionError.message" />
    <AppLoader v-if="rbacStore.isLoading" label="Loading roles..." />
    <AppErrorMessage v-else-if="rbacStore.error" title="Could not load roles" :message="rbacStore.error.message" />

    <div v-else class="admin-roles-page__grid">
      <AdminRolesTable
        :roles="rbacStore.roles"
        :selected-role-id="rbacStore.selectedRoleId"
        :can-manage="canManageRoles"
        :loading="rbacStore.isSaving"
        @select-role="rbacStore.selectRole"
        @update-role="rbacStore.updateRole"
        @delete-role="rbacStore.deleteRole"
      />
      <PermissionAssignmentPanel
        v-if="canManageRoles && canReadPermissions"
        :selected-role-id="rbacStore.selectedRoleId"
        :selected-role="selectedRole"
        :role-permissions="rbacStore.selectedRolePermissions"
        :permissions="rbacStore.permissions"
        :loading="rbacStore.isSaving"
        @assign-permission="rbacStore.assignPermissionToSelectedRole"
        @remove-permission="rbacStore.removePermissionFromSelectedRole"
      />
    </div>
  </main>
</template>

<style scoped>
.admin-roles-page,
.admin-roles-page__grid {
  display: grid;
  gap: 20px;
}
</style>
