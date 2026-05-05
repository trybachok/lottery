import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { Draw } from '@/shared/api/generated/types.gen'
import { listDraws } from '../api/draws.api'

export const useDrawsStore = defineStore('draws', () => {
  const items = ref<Draw[]>([])
  const isLoading = ref(false)
  const error = ref<FrontendApiError | null>(null)

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

  return {
    items,
    isLoading,
    error,
    loadDraws,
  }
})
