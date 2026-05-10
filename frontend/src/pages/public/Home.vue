<script setup lang="ts">
import { onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import HomeLayoutRenderer from '@/features/home/ui/HomeLayoutRenderer.vue'
import { useHomeStore } from '@/features/home/model/home.store'

const homeStore = useHomeStore()

onMounted(() => {
  void homeStore.load()
})
</script>

<template>
  <AppLoader v-if="homeStore.isLoading" class="home-page__loader" label="Loading home page..." />
  <main v-else-if="homeStore.error" class="home-page__error">
    <AppErrorMessage title="Could not load home page" :message="homeStore.error.message" />
  </main>
  <HomeLayoutRenderer v-else-if="homeStore.homePage" :template="homeStore.homePage.template" />
</template>

<style scoped>
.home-page__loader,
.home-page__error {
  min-height: 100vh;
  padding: 40px;
}
</style>
