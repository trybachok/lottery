<script setup lang="ts">
import { onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AdminUserCreateForm from '@/features/admin-rbac/ui/AdminUserCreateForm.vue'
import AdminUsersTable from '@/features/admin-rbac/ui/AdminUsersTable.vue'
import RoleAssignmentPanel from '@/features/admin-rbac/ui/RoleAssignmentPanel.vue'
import { useAdminRbacStore } from '@/features/admin-rbac/model/adminRbac.store'
import type { AdminUserRequestWritable } from '@/shared/api/generated/types.gen'

const rbacStore = useAdminRbacStore()

onMounted(() => {
  void rbacStore.loadAll()
})

async function createUser(request: AdminUserRequestWritable): Promise<void> {
  await rbacStore.createUser(request)
}
</script>

<template>
  <main class="admin-users-page">
    <BaseCard title="Create user" description="Create an administrative or client user.">
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
        @select-user="rbacStore.selectUser"
      />
      <RoleAssignmentPanel
        :selected-user-id="rbacStore.selectedUserId"
        :user-roles="rbacStore.selectedUserRoles"
        :loading="rbacStore.isSaving"
        @assign-role="rbacStore.assignRoleToSelectedUser"
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
