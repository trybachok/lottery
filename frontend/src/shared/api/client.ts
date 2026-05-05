import type { Auth } from './generated/core/auth.gen'
import { client as generatedApiClient } from './generated/client.gen'
import { mapApiError, type FrontendApiError } from './errors'

const accessTokenStorageKey = 'lottery.accessToken'
let responseInterceptorId: number | undefined
let unauthorizedHandler: ((error: FrontendApiError) => void) | undefined

export { generatedApiClient }
export type { FrontendApiError }

export function configureApiClient(): void {
  generatedApiClient.setConfig({
    baseURL: getApiBaseUrl(),
    headers: {
      Accept: 'application/json',
    },
    auth: resolveAuthToken,
    throwOnError: true,
  })

  if (responseInterceptorId !== undefined) {
    generatedApiClient.instance.interceptors.response.eject(responseInterceptorId)
  }

  responseInterceptorId = generatedApiClient.instance.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
      const apiError = mapApiError(error)

      if (apiError.status === 401) {
        clearAccessToken()
        unauthorizedHandler?.(apiError)
      }

      return Promise.reject(apiError)
    },
  )
}

export function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
}

export function setAccessToken(token: string): void {
  getStorage()?.setItem(accessTokenStorageKey, token)
}

export function getAccessToken(): string | null {
  return getStorage()?.getItem(accessTokenStorageKey) ?? null
}

export function clearAccessToken(): void {
  getStorage()?.removeItem(accessTokenStorageKey)
}

export function setUnauthorizedHandler(handler: ((error: FrontendApiError) => void) | undefined): void {
  unauthorizedHandler = handler
}

function resolveAuthToken(_auth: Auth): string | undefined {
  return getAccessToken() ?? undefined
}

function getStorage(): Storage | undefined {
  if (typeof window === 'undefined') {
    return undefined
  }

  return window.localStorage
}
