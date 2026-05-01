import { client } from './generated/client.gen'

export function configureApiClient(): void {
    client.setConfig({
        baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
        withCredentials: true,
    })
}