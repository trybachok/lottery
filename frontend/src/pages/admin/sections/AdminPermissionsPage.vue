<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AdminPermissionCreateForm from '@/features/admin-rbac/ui/AdminPermissionCreateForm.vue'
import AdminPermissionsTable from '@/features/admin-rbac/ui/AdminPermissionsTable.vue'
import { useAdminRbacStore } from '@/features/admin-rbac/model/adminRbac.store'
import { useAuthStore } from '@/features/auth/model/auth.store'
import { hasPermission } from '@/shared/lib/permissions/hasPermission'
import { PermissionCodes } from '@/shared/lib/permissions/permissionCodes'
import type { PermissionRequest } from '@/shared/api/generated/types.gen'

const rbacStore = useAdminRbacStore()
const authStore = useAuthStore()
const canManagePermissions = computed(
  () => authStore.roleCodes.includes('ADMIN') || hasPermission(authStore.permissions, [PermissionCodes.PERMISSION_MANAGE]),
)

onMounted(() => {
  void rbacStore.loadPermissions()
})

async function createPermission(request: PermissionRequest): Promise<void> {
  await rbacStore.createPermission(request)
}
</script>

<template>
  <main class="admin-permissions-page">
    <BaseCard v-if="canManagePermissions" title="Create permission" description="Create a permission code for future RBAC configuration.">
      <AdminPermissionCreateForm
        :loading="rbacStore.isSaving"
        :error-message="rbacStore.actionError?.message"
        @submit="createPermission"
      />
    </BaseCard>

    <AppErrorMessage v-if="rbacStore.actionError" title="Action failed" :message="rbacStore.actionError.message" />
    <AppLoader v-if="rbacStore.isLoading" label="Loading permissions..." />
    <AppErrorMessage v-else-if="rbacStore.error" title="Could not load permissions" :message="rbacStore.error.message" />
    <AdminPermissionsTable
      v-else
      :permissions="rbacStore.permissions"
      :can-manage="canManagePermissions"
      :loading="rbacStore.isSaving"
      @update-permission="rbacStore.updatePermission"
      @delete-permission="rbacStore.deletePermission"
    />
  </main>
</template>

<style scoped>
.admin-permissions-page {
  display: grid;
  gap: 20px;
}
</style>
