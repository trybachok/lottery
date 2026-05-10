import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getHomePage } from '@/shared/api/generated/sdk.gen'
import type { UiTheme } from '@/shared/api/generated/types.gen'

const selectedThemeStorageKey = 'lottery.theme.id'

type ThemeColors = {
  background?: string
  surface?: string
  surfaceMutedText?: string
  text?: string
  mutedText?: string
  primary?: string
  primaryText?: string
  accent?: string
  border?: string
  banner?: string
  sidebar?: string
}

export const useThemeStore = defineStore('theme', () => {
  const themes = ref<UiTheme[]>([])
  const defaultTheme = ref<UiTheme | null>(null)
  const selectedThemeId = ref<string | null>(null)
  const isLoading = ref(false)

  const selectedTheme = computed(() => {
    return themes.value.find((theme) => theme.id === selectedThemeId.value) ?? defaultTheme.value ?? themes.value[0] ?? null
  })

  function restoreSelection(): void {
    selectedThemeId.value = getStorage()?.getItem(selectedThemeStorageKey) ?? null
  }

  async function loadAvailableThemes(): Promise<void> {
    isLoading.value = true

    try {
      const response = await getHomePage({
        throwOnError: true,
      })
      setAvailableThemes(response.data.themes, response.data.defaultTheme)
    } catch (error) {
      console.warn('Could not load UI themes', error)
    } finally {
      isLoading.value = false
    }
  }

  function setAvailableThemes(nextThemes: UiTheme[], nextDefaultTheme: UiTheme): void {
    themes.value = nextThemes
    defaultTheme.value = nextDefaultTheme

    const storedThemeId = selectedThemeId.value ?? getStorage()?.getItem(selectedThemeStorageKey)
    selectedThemeId.value = storedThemeId && nextThemes.some((theme) => theme.id === storedThemeId)
      ? storedThemeId
      : nextDefaultTheme.id
    applySelectedTheme()
  }

  function selectTheme(themeId: string): void {
    if (!themes.value.some((theme) => theme.id === themeId)) {
      return
    }

    selectedThemeId.value = themeId
    getStorage()?.setItem(selectedThemeStorageKey, themeId)
    applySelectedTheme()
  }

  function applySelectedTheme(): void {
    if (!selectedTheme.value) {
      return
    }

    const colors = readColors(selectedTheme.value.tokens)
    setCssVariable('--color-bg', colors.background)
    setCssVariable('--color-surface', colors.surface)
    setCssVariable('--color-surface-muted-text', resolveSurfaceMutedText(selectedTheme.value.tokens, colors))
    setCssVariable('--color-text', colors.text)
    setCssVariable('--color-text-muted', colors.mutedText)
    setCssVariable('--color-primary', colors.primary)
    setCssVariable('--color-primary-text', colors.primaryText)
    setCssVariable('--color-primary-hover', colors.primary)
    setCssVariable('--color-primary-soft', colors.banner)
    setCssVariable('--color-border', colors.border)
    setCssVariable('--color-border-strong', colors.border)
    setCssVariable('--color-home-banner', colors.banner)
    setCssVariable('--color-home-sidebar', colors.sidebar)
    setCssVariable('--color-accent', colors.accent)
    document.documentElement.dataset.theme = String(selectedTheme.value.tokens.mode ?? selectedTheme.value.name)
  }

  function readColors(tokens: UiTheme['tokens']): ThemeColors {
    const colors = tokens.colors
    return typeof colors === 'object' && colors !== null ? (colors as ThemeColors) : {}
  }

  function setCssVariable(name: string, value: string | undefined): void {
    if (value) {
      document.documentElement.style.setProperty(name, value)
    }
  }

  function resolveSurfaceMutedText(tokens: UiTheme['tokens'], colors: ThemeColors): string | undefined {
    if (colors.surfaceMutedText) {
      return colors.surfaceMutedText
    }

    return tokens.mode === 'dark' ? colors.primaryText : colors.text
  }

  return {
    themes,
    defaultTheme,
    selectedThemeId,
    selectedTheme,
    isLoading,
    restoreSelection,
    loadAvailableThemes,
    setAvailableThemes,
    selectTheme,
    applySelectedTheme,
  }
})

function getStorage(): Storage | undefined {
  if (typeof window === 'undefined') {
    return undefined
  }

  return window.localStorage
}
