declare module 'swagger-ui-dist/swagger-ui-es-bundle.js' {
  type SwaggerUiOptions = {
    domNode: HTMLElement
    url: string
    deepLinking?: boolean
    displayRequestDuration?: boolean
    docExpansion?: 'list' | 'full' | 'none'
    filter?: boolean
    persistAuthorization?: boolean
    tryItOutEnabled?: boolean
    defaultModelsExpandDepth?: number
    syntaxHighlight?: {
      theme?: string
    }
    requestInterceptor?: (request: SwaggerUiRequest) => SwaggerUiRequest
  }

  type SwaggerUiInstance = {
    getSystem?: () => unknown
  }

  type SwaggerUiRequest = {
    url: string
    headers?: Record<string, string>
  }

  export default function SwaggerUI(options: SwaggerUiOptions): SwaggerUiInstance
}
