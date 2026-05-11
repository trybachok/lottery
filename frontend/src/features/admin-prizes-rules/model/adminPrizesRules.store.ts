import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { Prize, PrizeRequest, WinningRule, WinningRuleRequest } from '@/shared/api/generated/types.gen'
import {
  createAdminPrize,
  listAdminPrizes,
  listAdminWinningRules,
  replaceAdminWinningRules,
  updateAdminPrize,
} from '../api/adminPrizesRules.api'

export const useAdminPrizesRulesStore = defineStore('admin-prizes-rules', () => {
  const prizes = ref<Prize[]>([])
  const winningRules = ref<WinningRule[]>([])
  const selectedDrawId = ref('')
  const editingPrizeId = ref<string | null>(null)
  const isLoadingPrizes = ref(false)
  const isLoadingRules = ref(false)
  const isSavingPrize = ref(false)
  const isSavingRules = ref(false)
  const error = ref<FrontendApiError | null>(null)
  const actionError = ref<FrontendApiError | null>(null)
  const lastSavedMessage = ref<string | null>(null)

  async function loadPrizes(): Promise<void> {
    isLoadingPrizes.value = true
    error.value = null

    try {
      prizes.value = await listAdminPrizes()
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoadingPrizes.value = false
    }
  }

  async function savePrize(request: PrizeRequest): Promise<Prize | null> {
    isSavingPrize.value = true
    actionError.value = null
    lastSavedMessage.value = null

    try {
      const prize = editingPrizeId.value
        ? await updateAdminPrize(editingPrizeId.value, request)
        : await createAdminPrize(request)
      prizes.value = editingPrizeId.value
        ? prizes.value.map((item) => (item.id === prize.id ? prize : item))
        : [prize, ...prizes.value]
      editingPrizeId.value = null
      lastSavedMessage.value = 'Prize saved.'
      return prize
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      isSavingPrize.value = false
    }
  }

  async function loadWinningRules(drawId: string): Promise<void> {
    selectedDrawId.value = drawId
    winningRules.value = []
    if (!drawId) {
      return
    }

    isLoadingRules.value = true
    actionError.value = null

    try {
      winningRules.value = await listAdminWinningRules(drawId)
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
    } finally {
      isLoadingRules.value = false
    }
  }

  async function saveWinningRules(drawId: string, rules: WinningRuleRequest[]): Promise<WinningRule[] | null> {
    isSavingRules.value = true
    actionError.value = null
    lastSavedMessage.value = null

    try {
      const savedRules = await replaceAdminWinningRules(drawId, rules)
      selectedDrawId.value = drawId
      winningRules.value = savedRules
      lastSavedMessage.value = 'Winning rules saved.'
      return savedRules
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      isSavingRules.value = false
    }
  }

  function editPrize(prize: Prize): void {
    editingPrizeId.value = prize.id
  }

  function clearPrizeEdit(): void {
    editingPrizeId.value = null
  }

  function clearFeedback(): void {
    actionError.value = null
    lastSavedMessage.value = null
  }

  return {
    prizes,
    winningRules,
    selectedDrawId,
    editingPrizeId,
    isLoadingPrizes,
    isLoadingRules,
    isSavingPrize,
    isSavingRules,
    error,
    actionError,
    lastSavedMessage,
    loadPrizes,
    savePrize,
    loadWinningRules,
    saveWinningRules,
    editPrize,
    clearPrizeEdit,
    clearFeedback,
  }
})
