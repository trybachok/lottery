import {
  adminCreateUiTemplate,
  adminCreateUiTheme,
  adminGetHomePageSettings,
  adminListUiTemplates,
  adminListUiThemes,
  adminSetDefaultUiTheme,
  adminUpdateHomePageSettings,
  adminUpdateUiTemplate,
  adminUpdateUiTheme,
} from '@/shared/api/generated/sdk.gen'
import type {
  HomePageSettings,
  HomePageSettingsRequest,
  UiTemplate,
  UiTemplateRequest,
  UiTheme,
  UiThemeRequest,
} from '@/shared/api/generated/types.gen'

export async function listAdminUiThemes(): Promise<UiTheme[]> {
  const response = await adminListUiThemes({
    query: { limit: 100, offset: 0 },
    throwOnError: true,
  })

  return response.data.items
}

export async function createAdminUiTheme(request: UiThemeRequest): Promise<UiTheme> {
  const response = await adminCreateUiTheme({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function updateAdminUiTheme(themeId: string, request: UiThemeRequest): Promise<UiTheme> {
  const response = await adminUpdateUiTheme({
    path: { themeId },
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function setAdminDefaultUiTheme(themeId: string): Promise<UiTheme> {
  const response = await adminSetDefaultUiTheme({
    path: { themeId },
    throwOnError: true,
  })

  return response.data
}

export async function listAdminUiTemplates(): Promise<UiTemplate[]> {
  const response = await adminListUiTemplates({
    query: { limit: 100, offset: 0 },
    throwOnError: true,
  })

  return response.data.items
}

export async function createAdminUiTemplate(request: UiTemplateRequest): Promise<UiTemplate> {
  const response = await adminCreateUiTemplate({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function updateAdminUiTemplate(templateId: string, request: UiTemplateRequest): Promise<UiTemplate> {
  const response = await adminUpdateUiTemplate({
    path: { templateId },
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function getAdminHomePageSettings(): Promise<HomePageSettings> {
  const response = await adminGetHomePageSettings({
    throwOnError: true,
  })

  return response.data
}

export async function updateAdminHomePageSettings(request: HomePageSettingsRequest): Promise<HomePageSettings> {
  const response = await adminUpdateHomePageSettings({
    body: request,
    throwOnError: true,
  })

  return response.data
}
