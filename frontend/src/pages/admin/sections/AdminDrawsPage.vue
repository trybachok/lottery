<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import AdminDrawCreateForm from '@/features/admin-draws/ui/AdminDrawCreateForm.vue'
import AdminDrawsTable from '@/features/admin-draws/ui/AdminDrawsTable.vue'
import { useAdminDrawsStore } from '@/features/admin-draws/model/adminDraws.store'
import AdminPrizesRulesPanel from '@/features/admin-prizes-rules/ui/AdminPrizesRulesPanel.vue'
import { useAdminPrizesRulesStore } from '@/features/admin-prizes-rules/model/adminPrizesRules.store'
import { hasPermission } from '@/shared/lib/permissions/hasPermission'
import type { CreateDrawRequest, PrizeRequest, WinningRuleRequest } from '@/shared/api/generated/types.gen'

const adminDrawsStore = useAdminDrawsStore()
const adminPrizesRulesStore = useAdminPrizesRulesStore()
const authStore = useAuthStore()

const isAdmin = computed(() => authStore.roleCodes.includes('ADMIN'))
const isManager = computed(() => authStore.roleCodes.includes('MANAGER'))
const canCreateDraw = computed(() => isAdmin.value || hasPermission(authStore.permissions, ['draw.create']))
const canUpdateDraw = computed(() => isAdmin.value || hasPermission(authStore.permissions, ['draw.update']))
const canRunDraw = computed(() => isAdmin.value || hasPermission(authStore.permissions, ['draw.run']))
const canActivateDraw = computed(() => isAdmin.value || (isManager.value && hasPermission(authStore.permissions, ['draw.update'])))

onMounted(() => {
  void adminDrawsStore.loadDraws()
  if (canUpdateDraw.value) {
    void adminPrizesRulesStore.loadPrizes()
  }
})

async function createDraw(request: CreateDrawRequest): Promise<void> {
  await adminDrawsStore.createDraw(request)
}

async function savePrize(request: PrizeRequest): Promise<void> {
  await adminPrizesRulesStore.savePrize(request)
}

async function saveRules(drawId: string, rules: WinningRuleRequest[]): Promise<void> {
  await adminPrizesRulesStore.saveWinningRules(drawId, rules)
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
      :activating-draw-id="adminDrawsStore.activatingDrawId"
      :closing-sales-draw-id="adminDrawsStore.closingSalesDrawId"
      :generating-draw-id="adminDrawsStore.generatingDrawId"
      :running-draw-id="adminDrawsStore.runningDrawId"
      :assigning-manager-draw-id="adminDrawsStore.assigningManagerDrawId"
      :current-user-id="authStore.user?.id"
      :is-admin="isAdmin"
      :is-manager="isManager"
      :can-activate="canActivateDraw"
      :can-close-sales="canActivateDraw"
      :can-generate="canRunDraw"
      :can-run="canRunDraw"
      :can-assign-manager="canUpdateDraw"
      @activate-draw="adminDrawsStore.activateDraw"
      @close-sales="adminDrawsStore.closeSales"
      @generate-winning-combination="adminDrawsStore.generateWinningCombination"
      @run-draw="adminDrawsStore.runDraw"
      @assign-manager="adminDrawsStore.assignManager"
    />

    <AdminPrizesRulesPanel
      v-if="canUpdateDraw"
      :draws="adminDrawsStore.items"
      :prizes="adminPrizesRulesStore.prizes"
      :winning-rules="adminPrizesRulesStore.winningRules"
      :selected-draw-id="adminPrizesRulesStore.selectedDrawId"
      :editing-prize-id="adminPrizesRulesStore.editingPrizeId"
      :loading-prizes="adminPrizesRulesStore.isLoadingPrizes"
      :loading-rules="adminPrizesRulesStore.isLoadingRules"
      :saving-prize="adminPrizesRulesStore.isSavingPrize"
      :saving-rules="adminPrizesRulesStore.isSavingRules"
      :error-message="adminPrizesRulesStore.error?.message"
      :action-error-message="adminPrizesRulesStore.actionError?.message"
      :feedback-message="adminPrizesRulesStore.lastSavedMessage"
      @save-prize="savePrize"
      @edit-prize="adminPrizesRulesStore.editPrize"
      @cancel-prize-edit="adminPrizesRulesStore.clearPrizeEdit"
      @load-rules="adminPrizesRulesStore.loadWinningRules"
      @save-rules="saveRules"
    />
  </main>
</template>

<style scoped>
.admin-draws-page {
  display: grid;
  gap: 20px;
}
</style>
