import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import { configureApiClient } from '@/shared/api/configureApiClient'
import { useAuthStore } from '@/features/auth/model/auth.store'
import { useThemeStore } from '@/features/theme/model/theme.store'
import router from './app/router'

const app = createApp(App)
const pinia = createPinia()

configureApiClient()

app.use(pinia)
app.use(router)

useAuthStore().restoreSession()
const themeStore = useThemeStore()
themeStore.restoreSelection()
void themeStore.loadAvailableThemes()

app.mount('#app')
