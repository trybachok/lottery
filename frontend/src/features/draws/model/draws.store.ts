import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { Draw, DrawResult } from '@/shared/api/generated/types.gen'
import { getDraw, getResult, listDraws } from '../api/draws.api'

export const useDrawsStore = defineStore('draws', () => {
  const items = ref<Draw[]>([])
  const selectedDraw = ref<Draw | null>(null)
  const selectedResult = ref<DrawResult | null>(null)
  const isLoading = ref(false)
  const isLoadingDetails = ref(false)
  const isLoadingResult = ref(false)
  const error = ref<FrontendApiError | null>(null)
  const detailsError = ref<FrontendApiError | null>(null)
  const resultError = ref<FrontendApiError | null>(null)

  async function loadDraws(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      items.value = await listDraws({ limit: 50, offset: 0 })
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function loadDrawDetails(drawId: string): Promise<void> {
    isLoadingDetails.value = true
    detailsError.value = null
    selectedDraw.value = null
    selectedResult.value = null

    try {
      selectedDraw.value = await getDraw(drawId)
      await loadDrawResult(drawId)
    } catch (caughtError) {
      detailsError.value = mapApiError(caughtError)
    } finally {
      isLoadingDetails.value = false
    }
  }

  async function loadDrawResult(drawId: string): Promise<void> {
    isLoadingResult.value = true
    resultError.value = null

    try {
      selectedResult.value = await getResult(drawId)
    } catch (caughtError) {
      const apiError = mapApiError(caughtError)
      selectedResult.value = null
      if (apiError.status !== 404) {
        resultError.value = apiError
      }
    } finally {
      isLoadingResult.value = false
    }
  }

  return {
    items,
    selectedDraw,
    selectedResult,
    isLoading,
    isLoadingDetails,
    isLoadingResult,
    error,
    detailsError,
    resultError,
    loadDraws,
    loadDrawDetails,
    loadDrawResult,
  }
})
