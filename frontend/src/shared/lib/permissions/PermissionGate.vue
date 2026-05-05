<script setup lang="ts">
import { computed } from 'vue'
import { hasPermission, type PermissionMode } from './hasPermission'

const props = withDefaults(
  defineProps<{
    userPermissions: string[]
    permissions?: string[]
    mode?: PermissionMode
  }>(),
  {
    permissions: undefined,
    mode: 'all',
  },
)

const allowed = computed(() => hasPermission(props.userPermissions, props.permissions, props.mode))
</script>

<template>
  <slot v-if="allowed" />
  <slot v-else name="fallback" />
</template>
