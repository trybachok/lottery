import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type {
  HomePageSettings,
  HomePageSettingsRequest,
  UiTemplate,
  UiTemplateRequest,
  UiTheme,
  UiThemeRequest,
} from '@/shared/api/generated/types.gen'
import {
  createAdminUiTemplate,
  createAdminUiTheme,
  getAdminHomePageSettings,
  listAdminUiTemplates,
  listAdminUiThemes,
  setAdminDefaultUiTheme,
  updateAdminHomePageSettings,
  updateAdminUiTemplate,
  updateAdminUiTheme,
} from '../api/adminUi.api'

export const useAdminUiStore = defineStore('admin-ui', () => {
  const themes = ref<UiTheme[]>([])
  const templates = ref<UiTemplate[]>([])
  const settings = ref<HomePageSettings | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<FrontendApiError | null>(null)
  const actionError = ref<FrontendApiError | null>(null)

  async function loadThemes(): Promise<void> {
    await loadResource(async () => {
      themes.value = await listAdminUiThemes()
    })
  }

  async function loadTemplates(): Promise<void> {
    await loadResource(async () => {
      templates.value = await listAdminUiTemplates()
    })
  }

  async function loadSettingsResources(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      const [loadedThemes, loadedTemplates, loadedSettings] = await Promise.all([
        listAdminUiThemes(),
        listAdminUiTemplates(),
        getAdminHomePageSettings(),
      ])
      themes.value = loadedThemes
      templates.value = loadedTemplates
      settings.value = loadedSettings
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function createTheme(request: UiThemeRequest): Promise<UiTheme | null> {
    return saveAction(async () => {
      const theme = await createAdminUiTheme(request)
      themes.value = [theme, ...themes.value.filter((item) => item.id !== theme.id)]
      normalizeDefaultTheme(theme)
      return theme
    })
  }

  async function updateTheme(themeId: string, request: UiThemeRequest): Promise<UiTheme | null> {
    return saveAction(async () => {
      const theme = await updateAdminUiTheme(themeId, request)
      themes.value = themes.value.map((item) => (item.id === theme.id ? theme : item))
      normalizeDefaultTheme(theme)
      return theme
    })
  }

  async function setDefaultTheme(themeId: string): Promise<void> {
    await saveAction(async () => {
      const theme = await setAdminDefaultUiTheme(themeId)
      normalizeDefaultTheme(theme)
      return null
    })
  }

  async function createTemplate(request: UiTemplateRequest): Promise<UiTemplate | null> {
    return saveAction(async () => {
      const template = await createAdminUiTemplate(request)
      templates.value = [template, ...templates.value.filter((item) => item.id !== template.id)]
      return template
    })
  }

  async function updateTemplate(templateId: string, request: UiTemplateRequest): Promise<UiTemplate | null> {
    return saveAction(async () => {
      const template = await updateAdminUiTemplate(templateId, request)
      templates.value = templates.value.map((item) => (item.id === template.id ? template : item))
      return template
    })
  }

  async function updateSettings(request: HomePageSettingsRequest): Promise<HomePageSettings | null> {
    return saveAction(async () => {
      const updatedSettings = await updateAdminHomePageSettings(request)
      settings.value = updatedSettings
      return updatedSettings
    })
  }

  async function loadResource(work: () => Promise<void>): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      await work()
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function saveAction<T>(work: () => Promise<T>): Promise<T | null> {
    isSaving.value = true
    actionError.value = null

    try {
      return await work()
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      isSaving.value = false
    }
  }

  function normalizeDefaultTheme(defaultTheme: UiTheme): void {
    if (!defaultTheme.defaultTheme) {
      return
    }

    themes.value = themes.value.map((theme) => ({
      ...theme,
      defaultTheme: theme.id === defaultTheme.id,
    }))
  }

  return {
    themes,
    templates,
    settings,
    isLoading,
    isSaving,
    error,
    actionError,
    loadThemes,
    loadTemplates,
    loadSettingsResources,
    createTheme,
    updateTheme,
    setDefaultTheme,
    createTemplate,
    updateTemplate,
    updateSettings,
  }
})
