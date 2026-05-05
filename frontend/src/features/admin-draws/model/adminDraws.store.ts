import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { CreateDrawRequest, Draw, RunDrawResponse } from '@/shared/api/generated/types.gen'
import {
  assignDrawManager,
  createAdminDraw,
  listAdminDraws,
  runAdminDraw,
} from '../api/adminDraws.api'

export const useAdminDrawsStore = defineStore('admin-draws', () => {
  const items = ref<Draw[]>([])
  const lastRunResult = ref<RunDrawResponse | null>(null)
  const isLoading = ref(false)
  const isCreating = ref(false)
  const runningDrawId = ref<string | null>(null)
  const assigningManagerDrawId = ref<string | null>(null)
  const error = ref<FrontendApiError | null>(null)
  const actionError = ref<FrontendApiError | null>(null)

  async function loadDraws(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      items.value = await listAdminDraws({ limit: 100, offset: 0 })
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function createDraw(request: CreateDrawRequest): Promise<Draw | null> {
    isCreating.value = true
    actionError.value = null

    try {
      const draw = await createAdminDraw(request)
      items.value = [draw, ...items.value]
      return draw
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      isCreating.value = false
    }
  }

  async function runDraw(drawId: string): Promise<RunDrawResponse | null> {
    runningDrawId.value = drawId
    actionError.value = null
    lastRunResult.value = null

    try {
      const result = await runAdminDraw(drawId)
      lastRunResult.value = result
      await loadDraws()
      return result
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      runningDrawId.value = null
    }
  }

  async function assignManager(drawId: string, managerId: string): Promise<Draw | null> {
    assigningManagerDrawId.value = drawId
    actionError.value = null

    try {
      const draw = await assignDrawManager(drawId, { managerId })
      items.value = items.value.map((item) => (item.id === draw.id ? draw : item))
      return draw
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      assigningManagerDrawId.value = null
    }
  }

  function clearActionError(): void {
    actionError.value = null
  }

  return {
    items,
    lastRunResult,
    isLoading,
    isCreating,
    runningDrawId,
    assigningManagerDrawId,
    error,
    actionError,
    loadDraws,
    createDraw,
    runDraw,
    assignManager,
    clearActionError,
  }
})
