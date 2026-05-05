import type { AxiosError } from 'axios'
import type { ErrorResponse } from './generated/types.gen'

export type ApiErrorKind =
  | 'unauthorized'
  | 'forbidden'
  | 'conflict'
  | 'server'
  | 'validation'
  | 'network'
  | 'unknown'

export type FrontendApiError = {
  kind: ApiErrorKind
  code: string
  message: string
  details: Record<string, unknown>
  requestId?: string
  status?: number
}

type ErrorPayload = Partial<ErrorResponse> & {
  error?: Partial<ErrorResponse['error']>
}

const defaultMessages: Record<ApiErrorKind, string> = {
  unauthorized: 'Session expired. Please sign in again.',
  forbidden: 'You do not have permission to perform this action.',
  conflict: 'The requested operation conflicts with the current state.',
  server: 'The server could not process the request.',
  validation: 'Please check the entered data.',
  network: 'Network error. Please try again.',
  unknown: 'Unexpected error. Please try again.',
}

export function mapApiError(error: unknown): FrontendApiError {
  if (isFrontendApiError(error)) {
    return error
  }

  const axiosError = error as AxiosError<ErrorPayload>
  const status = axiosError.response?.status
  const payload = axiosError.response?.data
  const backendError = payload?.error
  const kind = resolveErrorKind(status, backendError?.code, axiosError)

  return {
    kind,
    code: backendError?.code ?? resolveFallbackCode(kind, status),
    message: backendError?.message ?? defaultMessages[kind],
    details: backendError?.details ?? {},
    requestId: backendError?.requestId,
    status,
  }
}

export function isFrontendApiError(error: unknown): error is FrontendApiError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'kind' in error &&
    'code' in error &&
    'message' in error
  )
}

function resolveErrorKind(
  status: number | undefined,
  backendCode: string | undefined,
  error: AxiosError<ErrorPayload>,
): ApiErrorKind {
  if (!status && error.isAxiosError) {
    return 'network'
  }

  if (status === 401) {
    return 'unauthorized'
  }

  if (status === 403) {
    return 'forbidden'
  }

  if (status === 409) {
    return 'conflict'
  }

  if (status === 400 || backendCode?.includes('VALIDATION')) {
    return 'validation'
  }

  if (status !== undefined && status >= 500) {
    return 'server'
  }

  return 'unknown'
}

function resolveFallbackCode(kind: ApiErrorKind, status: number | undefined): string {
  if (status !== undefined) {
    return `HTTP_${status}`
  }

  return kind.toUpperCase()
}
