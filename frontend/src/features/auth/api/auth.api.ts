import { loginByPassword, registerUser } from '@/shared/api/generated/sdk.gen'
import type {
  AuthResponse,
  LoginRequestWritable,
  RegisterRequestWritable,
  User,
} from '@/shared/api/generated/types.gen'

export type LoginCredentials = LoginRequestWritable
export type RegisterCredentials = RegisterRequestWritable

export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  const response = await loginByPassword({
    body: credentials,
    throwOnError: true,
  })

  return response.data
}

export async function register(credentials: RegisterCredentials): Promise<User> {
  const response = await registerUser({
    body: credentials,
    throwOnError: true,
  })

  return response.data
}
