import {
  adminCreatePrize,
  adminListDrawWinningRules,
  adminListPrizes,
  adminReplaceDrawWinningRules,
  adminUpdatePrize,
} from '@/shared/api/generated/sdk.gen'
import type {
  Prize,
  PrizeRequest,
  WinningRule,
  WinningRuleRequest,
} from '@/shared/api/generated/types.gen'

export async function listAdminPrizes(): Promise<Prize[]> {
  const response = await adminListPrizes({
    query: {
      limit: 250,
      offset: 0,
    },
    throwOnError: true,
  })

  return response.data.items
}

export async function createAdminPrize(request: PrizeRequest): Promise<Prize> {
  const response = await adminCreatePrize({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function updateAdminPrize(prizeId: string, request: PrizeRequest): Promise<Prize> {
  const response = await adminUpdatePrize({
    path: { prizeId },
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function listAdminWinningRules(drawId: string): Promise<WinningRule[]> {
  const response = await adminListDrawWinningRules({
    path: { drawId },
    throwOnError: true,
  })

  return response.data.items
}

export async function replaceAdminWinningRules(
  drawId: string,
  rules: WinningRuleRequest[],
): Promise<WinningRule[]> {
  const response = await adminReplaceDrawWinningRules({
    path: { drawId },
    body: { rules },
    throwOnError: true,
  })

  return response.data.items
}
