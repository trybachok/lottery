<script setup lang="ts">
import { ref } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { Role } from '@/shared/api/generated/types.gen'

defineProps<{ selectedUserId?: string | null; userRoles: Role[]; loading?: boolean }>()
const emit = defineEmits<{ assignRole: [roleId: string] }>()
const roleId = ref('')

function assign(): void {
  if (!roleId.value.trim()) return
  emit('assignRole', roleId.value.trim())
  roleId.value = ''
}
</script>

<template>
  <BaseCard title="User roles" :description="selectedUserId ? 'Assign roles to the selected user.' : 'Select a user first.'">
    <div class="assignment-panel">
      <BaseInput id="assign-user-role" v-model="roleId" label="Role id" :disabled="!selectedUserId || loading" />
      <BaseButton :disabled="!selectedUserId" :loading="loading" @click="assign">Assign role</BaseButton>
      <p class="assignment-panel__list">
        Current: {{ userRoles.length ? userRoles.map((role) => role.code).join(', ') : '-' }}
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
