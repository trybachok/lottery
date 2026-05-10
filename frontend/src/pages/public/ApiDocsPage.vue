<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import SwaggerUI from 'swagger-ui-dist/swagger-ui-es-bundle.js'
import 'swagger-ui-dist/swagger-ui.css'
import { getAccessToken } from '@/shared/api/client'

const swaggerContainer = ref<HTMLElement | null>(null)
const docsError = ref<string | null>(null)
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const openApiUrl = `${apiBaseUrl.replace(/\/$/, '')}/openapi.yaml`

onMounted(() => {
  if (!swaggerContainer.value) {
    return
  }

  SwaggerUI({
    domNode: swaggerContainer.value,
    url: openApiUrl,
    deepLinking: true,
    displayRequestDuration: true,
    docExpansion: 'none',
    filter: true,
    persistAuthorization: true,
    tryItOutEnabled: true,
    defaultModelsExpandDepth: 1,
    syntaxHighlight: {
      theme: 'agate',
    },
    requestInterceptor: (request) => {
      const token = getAccessToken()
      if (token) {
        request.headers = {
          ...request.headers,
          Authorization: `Bearer ${token}`,
        }
      }
      return request
    },
  })
})

onBeforeUnmount(() => {
  if (swaggerContainer.value) {
    swaggerContainer.value.innerHTML = ''
  }
})

async function downloadYaml(): Promise<void> {
  docsError.value = null
  const token = getAccessToken()

  try {
    const response = await fetch(openApiUrl, {
      headers: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : undefined,
    })

    if (!response.ok) {
      docsError.value = `OpenAPI contract request failed with status ${response.status}.`
      return
    }

    const blobUrl = URL.createObjectURL(await response.blob())
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = 'lottery-openapi.yaml'
    link.click()
    URL.revokeObjectURL(blobUrl)
  } catch {
    docsError.value = 'OpenAPI contract request failed.'
  }
}
</script>

<template>
  <main class="api-docs-page">
    <header class="api-docs-header">
      <div>
        <p class="api-docs-kicker">Lottery API</p>
        <h1>OpenAPI Contracts</h1>
      </div>
      <nav class="api-docs-actions" aria-label="API documentation links">
        <button class="api-docs-link" type="button" @click="downloadYaml">
          YAML
        </button>
        <a class="api-docs-link" href="/" aria-label="Go to application home">
          App
        </a>
      </nav>
    </header>

    <p v-if="docsError" class="api-docs-error" role="alert">
      {{ docsError }}
    </p>

    <section class="api-docs-surface" aria-label="OpenAPI documentation">
      <div ref="swaggerContainer" class="api-docs-swagger" />
    </section>
  </main>
</template>

<style scoped>
.api-docs-page {
  min-height: 100vh;
  background: #f5f7fb;
  color: #172033;
}

.api-docs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px clamp(16px, 4vw, 48px);
  border-bottom: 1px solid #d7dde8;
  background: #ffffff;
}

.api-docs-kicker {
  margin: 0 0 4px;
  color: #166534;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.api-docs-header h1 {
  margin: 0;
  font-size: clamp(26px, 3vw, 38px);
  line-height: 1.1;
}

.api-docs-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.api-docs-link {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border: 1px solid #aeb8c9;
  border-radius: 6px;
  background: #ffffff;
  color: #18212f;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}

.api-docs-link:hover {
  border-color: #166534;
  color: #166534;
}

.api-docs-error {
  margin: 16px clamp(16px, 4vw, 48px) 0;
  padding: 12px 14px;
  border: 1px solid #fda29b;
  border-radius: 8px;
  background: #fff1f0;
  color: #b42318;
  font-size: 14px;
  font-weight: 700;
}

.api-docs-surface {
  padding: 0 clamp(8px, 2vw, 24px) 32px;
}

.api-docs-swagger {
  width: 100%;
}

:deep(.swagger-ui) {
  color: #172033;
  font-family:
    Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

:deep(.swagger-ui .topbar) {
  display: none;
}

:deep(.swagger-ui .info) {
  margin: 28px 0;
}

:deep(.swagger-ui .info .title) {
  color: #172033;
  font-size: 28px;
}

:deep(.swagger-ui .scheme-container) {
  margin: 0 0 20px;
  padding: 16px 20px;
  border: 1px solid #d7dde8;
  border-radius: 8px;
  box-shadow: none;
}

:deep(.swagger-ui .opblock-tag),
:deep(.swagger-ui .opblock) {
  border-radius: 8px;
}

:deep(.swagger-ui .opblock-tag) {
  border-bottom-color: #d7dde8;
  color: #172033;
}

:deep(.swagger-ui .opblock .opblock-summary) {
  min-height: 52px;
}

:deep(.swagger-ui .btn) {
  border-radius: 6px;
}

:deep(.swagger-ui input),
:deep(.swagger-ui select),
:deep(.swagger-ui textarea) {
  border-radius: 6px;
}

@media (max-width: 720px) {
  .api-docs-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .api-docs-actions,
  .api-docs-link {
    width: 100%;
  }
}
</style>
