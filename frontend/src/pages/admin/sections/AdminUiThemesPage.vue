<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import { useAdminUiStore } from '@/features/admin-ui/model/adminUi.store'
import type { JsonObject, UiTheme } from '@/shared/api/generated/types.gen'

const adminUiStore = useAdminUiStore()
const selectedThemeId = ref<string | null>(null)
const name = ref('')
const tokensJson = ref(defaultTokensJson())
const defaultTheme = ref(false)
const formError = ref<string | null>(null)
const selectedTheme = computed(() => adminUiStore.themes.find((theme) => theme.id === selectedThemeId.value) ?? null)

onMounted(() => {
  void adminUiStore.loadThemes()
})

function selectTheme(theme: UiTheme): void {
  selectedThemeId.value = theme.id
  name.value = theme.name
  tokensJson.value = JSON.stringify(theme.tokens, null, 2)
  defaultTheme.value = theme.defaultTheme
  formError.value = null
}

function resetForm(): void {
  selectedThemeId.value = null
  name.value = ''
  tokensJson.value = defaultTokensJson()
  defaultTheme.value = false
  formError.value = null
}

async function submit(): Promise<void> {
  formError.value = null
  const tokens = parseJson(tokensJson.value)
  if (!tokens) {
    return
  }

  const request = {
    name: name.value,
    tokens,
    defaultTheme: defaultTheme.value,
  }

  if (selectedThemeId.value) {
    await adminUiStore.updateTheme(selectedThemeId.value, request)
  } else {
    const created = await adminUiStore.createTheme(request)
    if (created) {
      selectTheme(created)
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

function defaultTokensJson(): string {
  return JSON.stringify(
    {
      mode: 'light',
      colors: {
        background: '#f7f8fb',
        surface: '#ffffff',
        text: '#18212f',
        mutedText: '#647084',
        primary: '#166534',
        primaryText: '#ffffff',
        accent: '#d97706',
        border: '#d7dde8',
        banner: '#dcfce7',
        sidebar: '#ffffff',
      },
    },
    null,
    2,
  )
}
</script>

<template>
  <main class="admin-ui-page">
    <BaseCard title="UI themes" description="Manage light, dark and custom client themes.">
      <form class="admin-ui-page__form" @submit.prevent="submit">
        <BaseInput v-model="name" label="Name" placeholder="Light" />
        <label class="admin-ui-page__field">
          <span>Tokens JSON</span>
          <textarea v-model="tokensJson" rows="14" spellcheck="false" />
        </label>
        <label class="admin-ui-page__checkbox">
          <input v-model="defaultTheme" type="checkbox" />
          <span>Set as default theme</span>
        </label>
        <AppErrorMessage v-if="formError" title="Invalid theme" :message="formError" />
        <AppErrorMessage v-if="adminUiStore.actionError" title="Action failed" :message="adminUiStore.actionError.message" />
        <div class="admin-ui-page__actions">
          <BaseButton type="submit" :loading="adminUiStore.isSaving">
            {{ selectedTheme ? 'Update theme' : 'Create theme' }}
          </BaseButton>
          <BaseButton type="button" variant="secondary" @click="resetForm">New theme</BaseButton>
        </div>
      </form>
    </BaseCard>

    <AppLoader v-if="adminUiStore.isLoading" label="Loading themes..." />
    <AppErrorMessage v-else-if="adminUiStore.error" title="Could not load themes" :message="adminUiStore.error.message" />

    <section v-else class="admin-ui-page__list">
      <article v-for="theme in adminUiStore.themes" :key="theme.id" class="admin-ui-page__item">
        <div>
          <h2>{{ theme.name }}</h2>
          <p>{{ theme.defaultTheme ? 'Default theme' : theme.id }}</p>
        </div>
        <div class="admin-ui-page__item-actions">
          <BaseButton size="sm" variant="secondary" @click="selectTheme(theme)">Edit</BaseButton>
          <BaseButton
            size="sm"
            variant="ghost"
            :disabled="theme.defaultTheme"
            :loading="adminUiStore.isSaving"
            @click="adminUiStore.setDefaultTheme(theme.id)"
          >
            Default
          </BaseButton>
        </div>
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

.admin-ui-page__checkbox,
.admin-ui-page__actions,
.admin-ui-page__item,
.admin-ui-page__item-actions {
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
