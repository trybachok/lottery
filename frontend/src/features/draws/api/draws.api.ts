import { getDrawById, getDrawResult, getDraws } from '@/shared/api/generated/sdk.gen'
import type { Draw, DrawResult } from '@/shared/api/generated/types.gen'

export async function listDraws(params: { limit?: number; offset?: number } = {}): Promise<Draw[]> {
  const response = await getDraws({
    query: params,
    throwOnError: true,
  })

  return response.data.items
}

export async function getDraw(drawId: string): Promise<Draw> {
  const response = await getDrawById({
    path: { drawId },
    throwOnError: true,
  })

  return response.data
}

export async function getResult(drawId: string): Promise<DrawResult> {
  const response = await getDrawResult({
    path: { drawId },
    throwOnError: true,
  })

  return response.data
}
