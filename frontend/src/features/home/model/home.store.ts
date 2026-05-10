import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { HomePage } from '@/shared/api/generated/types.gen'
import { useThemeStore } from '@/features/theme/model/theme.store'
import { loadHomePage } from '../api/home.api'

export const useHomeStore = defineStore('home', () => {
  const homePage = ref<HomePage | null>(null)
  const isLoading = ref(false)
  const error = ref<FrontendApiError | null>(null)

  async function load(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      const loadedHomePage = await loadHomePage()
      homePage.value = loadedHomePage
      useThemeStore().setAvailableThemes(loadedHomePage.themes, loadedHomePage.defaultTheme)
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  return {
    homePage,
    isLoading,
    error,
    load,
  }
})
