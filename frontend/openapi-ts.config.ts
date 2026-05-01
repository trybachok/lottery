import { defineConfig } from '@hey-api/openapi-ts'

export default defineConfig({
    input: '../backend/src/main/resources/openapi/openapi.yaml',
    output: 'src/shared/api/generated',
    plugins: [
        '@hey-api/client-axios',
    ],
})