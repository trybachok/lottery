<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from '@/widgets/site-shell/AppShell.vue'

const route = useRoute()
const usesHomeTemplate = computed(() => route.path === '/')
</script>

<template>
  <RouterView v-if="usesHomeTemplate" />
  <AppShell v-else />
</template>
