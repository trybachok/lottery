<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseSelect from '@/shared/ui/BaseSelect.vue'
import { useAdminUiStore } from '@/features/admin-ui/model/adminUi.store'

const adminUiStore = useAdminUiStore()
const activeTemplateId = ref('')
const defaultThemeId = ref('')

const templateOptions = computed(() =>
  adminUiStore.templates.map((template) => ({
    label: template.name,
    value: template.id,
  })),
)
const themeOptions = computed(() =>
  adminUiStore.themes.map((theme) => ({
    label: theme.defaultTheme ? `${theme.name} (default)` : theme.name,
    value: theme.id,
  })),
)

onMounted(() => {
  void adminUiStore.loadSettingsResources()
})

watch(
  () => adminUiStore.settings,
  (settings) => {
    if (!settings) {
      return
    }
    activeTemplateId.value = settings.activeTemplateId
    defaultThemeId.value = settings.defaultThemeId
  },
  { immediate: true },
)

async function save(): Promise<void> {
  await adminUiStore.updateSettings({
    activeTemplateId: activeTemplateId.value,
    defaultThemeId: defaultThemeId.value,
  })
}
</script>

<template>
  <main class="admin-settings-page">
    <AppLoader v-if="adminUiStore.isLoading" label="Loading settings..." />
    <AppErrorMessage v-else-if="adminUiStore.error" title="Could not load settings" :message="adminUiStore.error.message" />

    <BaseCard v-else title="Home page settings" description="Choose the client template and default theme.">
      <form class="admin-settings-page__form" @submit.prevent="save">
        <BaseSelect v-model="activeTemplateId" label="Active template" :options="templateOptions" />
        <BaseSelect v-model="defaultThemeId" label="Default theme" :options="themeOptions" />
        <AppErrorMessage v-if="adminUiStore.actionError" title="Action failed" :message="adminUiStore.actionError.message" />
        <div class="admin-settings-page__actions">
          <BaseButton type="submit" :loading="adminUiStore.isSaving">Save settings</BaseButton>
        </div>
      </form>
    </BaseCard>
  </main>
</template>

<style scoped>
.admin-settings-page,
.admin-settings-page__form {
  display: grid;
  gap: 18px;
}

.admin-settings-page__actions {
  display: flex;
  justify-content: flex-start;
}
</style>
