import {
  activateDraw,
  adminAssignDrawManager,
  closeDrawSales,
  createDraw,
  generateWinningCombination,
  getDraws,
  runDraw,
} from '@/shared/api/generated/sdk.gen'
import type {
  AssignManagerRequest,
  CreateDrawRequest,
  Draw,
  DrawResult,
  RunDrawResponse,
} from '@/shared/api/generated/types.gen'

export async function listAdminDraws(params: { limit?: number; offset?: number } = {}): Promise<Draw[]> {
  const response = await getDraws({
    query: params,
    throwOnError: true,
  })

  return response.data.items
}

export async function createAdminDraw(request: CreateDrawRequest): Promise<Draw> {
  const response = await createDraw({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function activateAdminDraw(drawId: string): Promise<Draw> {
  const response = await activateDraw({
    path: {
      drawId,
    },
    throwOnError: true,
  })

  return response.data
}

export async function closeAdminDrawSales(drawId: string): Promise<Draw> {
  const response = await closeDrawSales({
    path: {
      drawId,
    },
    throwOnError: true,
  })

  return response.data
}

export async function runAdminDraw(drawId: string): Promise<RunDrawResponse> {
  const response = await runDraw({
    path: {
      drawId,
    },
    throwOnError: true,
  })

  return response.data
}

export async function generateAdminWinningCombination(drawId: string): Promise<DrawResult> {
  const response = await generateWinningCombination({
    path: {
      drawId,
    },
    throwOnError: true,
  })

  return response.data
}

export async function assignDrawManager(drawId: string, request: AssignManagerRequest): Promise<Draw> {
  const response = await adminAssignDrawManager({
    path: {
      drawId,
    },
    body: request,
    throwOnError: true,
  })

  return response.data
}
