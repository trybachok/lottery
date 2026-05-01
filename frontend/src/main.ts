import {createApp} from 'vue'
import {createPinia} from 'pinia'
import './style.css'
import App from './App.vue'
import {configureApiClient} from '@/shared/api/configureApiClient';
import router from './app/router'


const app = createApp(App)

configureApiClient()

app.use(createPinia())
app.use(router)

app.mount('#app')
