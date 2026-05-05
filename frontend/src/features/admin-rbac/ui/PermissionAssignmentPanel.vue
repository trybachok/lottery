<script setup lang="ts">
import { ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { Permission } from '@/shared/api/generated/types.gen'

defineProps<{ selectedRoleId?: string | null; rolePermissions: Permission[]; loading?: boolean }>()
const emit = defineEmits<{ assignPermission: [permissionId: string] }>()
const permissionId = ref('')

function assign(): void {
  if (!permissionId.value.trim()) return
  emit('assignPermission', permissionId.value.trim())
  permissionId.value = ''
}
</script>

<template>
  <BaseCard title="Role permissions" :description="selectedRoleId ? 'Assign permissions to the selected role.' : 'Select a role first.'">
    <div class="assignment-panel">
      <BaseInput id="assign-role-permission" v-model="permissionId" label="Permission id" :disabled="!selectedRoleId || loading" />
      <BaseButton :disabled="!selectedRoleId" :loading="loading" @click="assign">Assign permission</BaseButton>
      <p class="assignment-panel__list">
        Current: {{ rolePermissions.length ? rolePermissions.map((permission) => permission.code).join(', ') : '-' }}
      </p>
    </div>
  </BaseCard>
</template>

<style scoped>
.assignment-panel {
  display: grid;
  gap: 12px;
}

.assignment-panel__list {
  margin: 0;
  color: var(--color-text-muted);
}
</style>
