import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
  setUnauthorizedHandler,
  type FrontendApiError,
} from '@/shared/api/client'
import { mapApiError } from '@/shared/api/errors'
import type { AuthResponse, User } from '@/shared/api/generated/types.gen'
import {
  login as loginRequest,
  register as registerRequest,
  type LoginCredentials,
  type RegisterCredentials,
} from '../api/auth.api'

const userStorageKey = 'lottery.auth.user'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const user = ref<User | null>(readStoredUser())
  const isLoading = ref(false)
  const error = ref<FrontendApiError | null>(null)

  const isAuthenticated = computed(() => Boolean(accessToken.value && user.value))

  function restoreSession(): void {
    accessToken.value = getAccessToken()
    user.value = readStoredUser()

    if (!accessToken.value || !user.value) {
      clearSession()
      return
    }

    setUnauthorizedHandler(() => {
      clearSession()
    })
  }

  async function login(credentials: LoginCredentials): Promise<AuthResponse> {
    return runAuthRequest(async () => {
      const authResponse = await loginRequest(credentials)
      persistAuthResponse(authResponse)
      return authResponse
    })
  }

  async function register(credentials: RegisterCredentials): Promise<AuthResponse> {
    return runAuthRequest(async () => {
      await registerRequest(credentials)

      const loginOrEmail = credentials.login?.trim() || credentials.email
      const authResponse = await loginRequest({
        loginOrEmail,
        password: credentials.password,
      })

      persistAuthResponse(authResponse)
      return authResponse
    })
  }

  function logout(): void {
    clearSession()
  }

  function clearError(): void {
    error.value = null
  }

  async function runAuthRequest<T>(request: () => Promise<T>): Promise<T> {
    isLoading.value = true
    error.value = null

    try {
      return await request()
    } catch (caughtError) {
      const apiError = mapApiError(caughtError)
      error.value = apiError
      throw apiError
    } finally {
      isLoading.value = false
    }
  }

  function persistAuthResponse(authResponse: AuthResponse): void {
    accessToken.value = authResponse.accessToken
    user.value = authResponse.user
    setAccessToken(authResponse.accessToken)
    writeStoredUser(authResponse.user)
  }

  function clearSession(): void {
    accessToken.value = null
    user.value = null
    error.value = null
    clearAccessToken()
    removeStoredUser()
  }

  setUnauthorizedHandler(() => {
    clearSession()
  })

  return {
    accessToken,
    user,
    isLoading,
    error,
    isAuthenticated,
    restoreSession,
    login,
    register,
    logout,
    clearError,
  }
})

function readStoredUser(): User | null {
  const rawUser = getStorage()?.getItem(userStorageKey)

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as User
  } catch {
    removeStoredUser()
    return null
  }
}

function writeStoredUser(user: User): void {
  getStorage()?.setItem(userStorageKey, JSON.stringify(user))
}

function removeStoredUser(): void {
  getStorage()?.removeItem(userStorageKey)
}

function getStorage(): Storage | undefined {
  if (typeof window === 'undefined') {
    return undefined
  }

  return window.localStorage
}
