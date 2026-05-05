import { getDraws } from '@/shared/api/generated/sdk.gen'
import type { Draw } from '@/shared/api/generated/types.gen'

export async function listDraws(params: { limit?: number; offset?: number } = {}): Promise<Draw[]> {
  const response = await getDraws({
    query: params,
    throwOnError: true,
  })

  return response.data.items
}
