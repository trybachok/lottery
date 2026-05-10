import { getHomePage } from '@/shared/api/generated/sdk.gen'
import type { HomePage } from '@/shared/api/generated/types.gen'

export async function loadHomePage(): Promise<HomePage> {
  const response = await getHomePage({
    throwOnError: true,
  })

  return response.data
}
