<script setup lang="ts">
import { onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AdminPermissionCreateForm from '@/features/admin-rbac/ui/AdminPermissionCreateForm.vue'
import AdminPermissionsTable from '@/features/admin-rbac/ui/AdminPermissionsTable.vue'
import { useAdminRbacStore } from '@/features/admin-rbac/model/adminRbac.store'
import type { PermissionRequest } from '@/shared/api/generated/types.gen'

const rbacStore = useAdminRbacStore()

onMounted(() => {
  void rbacStore.loadAll()
})

async function createPermission(request: PermissionRequest): Promise<void> {
  await rbacStore.createPermission(request)
}
</script>

<template>
  <main class="admin-permissions-page">
    <BaseCard title="Create permission" description="Create a permission code for future RBAC configuration.">
      <AdminPermissionCreateForm
        :loading="rbacStore.isSaving"
        :error-message="rbacStore.actionError?.message"
        @submit="createPermission"
      />
    </BaseCard>

    <AppErrorMessage v-if="rbacStore.actionError" title="Action failed" :message="rbacStore.actionError.message" />
    <AppLoader v-if="rbacStore.isLoading" label="Loading permissions..." />
    <AppErrorMessage v-else-if="rbacStore.error" title="Could not load permissions" :message="rbacStore.error.message" />
    <AdminPermissionsTable v-else :permissions="rbacStore.permissions" />
  </main>
</template>

<style scoped>
.admin-permissions-page {
  display: grid;
  gap: 20px;
}
</style>
