<script setup lang="ts">
import { onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AdminRoleCreateForm from '@/features/admin-rbac/ui/AdminRoleCreateForm.vue'
import AdminRolesTable from '@/features/admin-rbac/ui/AdminRolesTable.vue'
import PermissionAssignmentPanel from '@/features/admin-rbac/ui/PermissionAssignmentPanel.vue'
import { useAdminRbacStore } from '@/features/admin-rbac/model/adminRbac.store'
import type { RoleRequest } from '@/shared/api/generated/types.gen'

const rbacStore = useAdminRbacStore()

onMounted(() => {
  void rbacStore.loadAll()
})

async function createRole(request: RoleRequest): Promise<void> {
  await rbacStore.createRole(request)
}
</script>

<template>
  <main class="admin-roles-page">
    <BaseCard title="Create role" description="Create a role and then assign permissions to it.">
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
        @select-role="rbacStore.selectRole"
      />
      <PermissionAssignmentPanel
        :selected-role-id="rbacStore.selectedRoleId"
        :role-permissions="rbacStore.selectedRolePermissions"
        :loading="rbacStore.isSaving"
        @assign-permission="rbacStore.assignPermissionToSelectedRole"
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
