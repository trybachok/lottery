<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import AdminDrawCreateForm from '@/features/admin-draws/ui/AdminDrawCreateForm.vue'
import AdminDrawsTable from '@/features/admin-draws/ui/AdminDrawsTable.vue'
import { useAdminDrawsStore } from '@/features/admin-draws/model/adminDraws.store'
import { hasPermission } from '@/shared/lib/permissions/hasPermission'
import type { CreateDrawRequest } from '@/shared/api/generated/types.gen'

const adminDrawsStore = useAdminDrawsStore()
const authStore = useAuthStore()

const isAdmin = computed(() => authStore.roleCodes.includes('ADMIN'))
const canCreateDraw = computed(() => isAdmin.value || hasPermission(authStore.permissions, ['draw.create']))
const canUpdateDraw = computed(() => isAdmin.value || hasPermission(authStore.permissions, ['draw.update']))
const canRunDraw = computed(() => isAdmin.value || hasPermission(authStore.permissions, ['draw.run']))

onMounted(() => {
  void adminDrawsStore.loadDraws()
})

async function createDraw(request: CreateDrawRequest): Promise<void> {
  await adminDrawsStore.createDraw(request)
}
</script>

<template>
  <main class="admin-draws-page">
    <BaseCard
      v-if="canCreateDraw"
      title="Create draw"
      description="Create a draw and prepare its combination schema."
    >
      <AdminDrawCreateForm
        :loading="adminDrawsStore.isCreating"
        :error-message="adminDrawsStore.actionError?.message"
        :can-assign-manager="isAdmin"
        :default-manager-id="authStore.user?.id"
        @submit="createDraw"
      />
    </BaseCard>
    <AppErrorMessage
      v-else
      title="Create draw unavailable"
      message="Your account can view this admin section, but it does not have draw.create."
    />

    <AppErrorMessage
      v-if="adminDrawsStore.actionError"
      title="Action failed"
      :message="adminDrawsStore.actionError.message"
    />

    <BaseCard
      v-if="adminDrawsStore.lastGeneratedResult"
      title="Winning combination generated"
      :description="`Combination: ${adminDrawsStore.lastGeneratedResult.winningCombinationValues.join(', ')}`"
    />

    <BaseCard
      v-if="adminDrawsStore.lastRunResult"
      title="Draw completed"
      :description="`Processed ${adminDrawsStore.lastRunResult.processedTickets} tickets. Winners: ${adminDrawsStore.lastRunResult.winningTickets}.`"
    />

    <AppLoader v-if="adminDrawsStore.isLoading" label="Loading draws..." />
    <AppErrorMessage
      v-else-if="adminDrawsStore.error"
      title="Could not load draws"
      :message="adminDrawsStore.error.message"
    />
    <AdminDrawsTable
      v-else
      :draws="adminDrawsStore.items"
      :generating-draw-id="adminDrawsStore.generatingDrawId"
      :running-draw-id="adminDrawsStore.runningDrawId"
      :assigning-manager-draw-id="adminDrawsStore.assigningManagerDrawId"
      :can-generate="canRunDraw"
      :can-run="canRunDraw"
      :can-assign-manager="canUpdateDraw"
      @generate-winning-combination="adminDrawsStore.generateWinningCombination"
      @run-draw="adminDrawsStore.runDraw"
      @assign-manager="adminDrawsStore.assignManager"
    />
  </main>
</template>

<style scoped>
.admin-draws-page {
  display: grid;
  gap: 20px;
}
</style>
