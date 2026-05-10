<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import { useAdminUiStore } from '@/features/admin-ui/model/adminUi.store'
import type { JsonObject, UiTemplate } from '@/shared/api/generated/types.gen'

const adminUiStore = useAdminUiStore()
const selectedTemplateId = ref<string | null>(null)
const name = ref('')
const layoutJson = ref(defaultLayoutJson())
const formError = ref<string | null>(null)
const selectedTemplate = computed(
  () => adminUiStore.templates.find((template) => template.id === selectedTemplateId.value) ?? null,
)

onMounted(() => {
  void adminUiStore.loadTemplates()
})

function selectTemplate(template: UiTemplate): void {
  selectedTemplateId.value = template.id
  name.value = template.name
  layoutJson.value = JSON.stringify(template.layout, null, 2)
  formError.value = null
}

function resetForm(): void {
  selectedTemplateId.value = null
  name.value = ''
  layoutJson.value = defaultLayoutJson()
  formError.value = null
}

async function submit(): Promise<void> {
  formError.value = null
  const layout = parseJson(layoutJson.value)
  if (!layout) {
    return
  }

  const request = {
    name: name.value,
    layout,
  }

  if (selectedTemplateId.value) {
    await adminUiStore.updateTemplate(selectedTemplateId.value, request)
  } else {
    const created = await adminUiStore.createTemplate(request)
    if (created) {
      selectTemplate(created)
    }
  }
}

function parseJson(value: string): JsonObject | null {
  try {
    const parsed = JSON.parse(value) as unknown
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      formError.value = 'JSON must be an object.'
      return null
    }
    return parsed as JsonObject
  } catch {
    formError.value = 'JSON is invalid.'
    return null
  }
}

function defaultLayoutJson(): string {
  return JSON.stringify(
    {
      version: 1,
      regions: {
        banner: { type: 'banner', title: 'Welcome', subtitle: 'Configurable home page.' },
        sidebar: { type: 'sidebar', title: 'Menu', blocks: [{ type: 'link', label: 'Tickets', to: '/tickets' }] },
        main: { type: 'main', title: 'Home', blocks: [{ type: 'card', title: 'Card', text: 'Text' }] },
        footer: { type: 'footer', text: 'Lottery system' },
      },
    },
    null,
    2,
  )
}
</script>

<template>
  <main class="admin-ui-page">
    <BaseCard title="Home page templates" description="Edit the banner, sidebar, main area and footer layout.">
      <form class="admin-ui-page__form" @submit.prevent="submit">
        <BaseInput v-model="name" label="Name" placeholder="Default home page" />
        <label class="admin-ui-page__field">
          <span>Layout JSON</span>
          <textarea v-model="layoutJson" rows="20" spellcheck="false" />
        </label>
        <AppErrorMessage v-if="formError" title="Invalid template" :message="formError" />
        <AppErrorMessage v-if="adminUiStore.actionError" title="Action failed" :message="adminUiStore.actionError.message" />
        <div class="admin-ui-page__actions">
          <BaseButton type="submit" :loading="adminUiStore.isSaving">
            {{ selectedTemplate ? 'Update template' : 'Create template' }}
          </BaseButton>
          <BaseButton type="button" variant="secondary" @click="resetForm">New template</BaseButton>
        </div>
      </form>
    </BaseCard>

    <AppLoader v-if="adminUiStore.isLoading" label="Loading templates..." />
    <AppErrorMessage v-else-if="adminUiStore.error" title="Could not load templates" :message="adminUiStore.error.message" />

    <section v-else class="admin-ui-page__list">
      <article v-for="template in adminUiStore.templates" :key="template.id" class="admin-ui-page__item">
        <div>
          <h2>{{ template.name }}</h2>
          <p>{{ template.id }}</p>
        </div>
        <BaseButton size="sm" variant="secondary" @click="selectTemplate(template)">Edit</BaseButton>
      </article>
    </section>
  </main>
</template>

<style scoped>
.admin-ui-page,
.admin-ui-page__form,
.admin-ui-page__list {
  display: grid;
  gap: 18px;
}

.admin-ui-page__field {
  display: grid;
  gap: 6px;
  font-weight: 650;
}

.admin-ui-page__field textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  padding: 12px;
  resize: vertical;
}

.admin-ui-page__actions,
.admin-ui-page__item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-ui-page__item {
  justify-content: space-between;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 16px;
}

.admin-ui-page__item h2,
.admin-ui-page__item p {
  margin: 0;
}

.admin-ui-page__item p {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}
</style>
