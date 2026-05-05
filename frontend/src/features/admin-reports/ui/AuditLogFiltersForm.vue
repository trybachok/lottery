<script setup lang="ts">
import { reactive } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { AuditLogFilters } from '../api/adminReports.api'

defineProps<{
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [filters: AuditLogFilters]
}>()

const form = reactive({
  userId: '',
  action: '',
  entityType: '',
  entityId: '',
  dateFrom: '',
  dateTo: '',
})

function submitFilters(): void {
  const filters: AuditLogFilters = {
    limit: 50,
    offset: 0,
  }

  if (form.userId.trim()) {
    filters.userId = form.userId.trim()
  }

  if (form.action.trim()) {
    filters.action = form.action.trim()
  }

  if (form.entityType.trim()) {
    filters.entityType = form.entityType.trim()
  }

  if (form.entityId.trim()) {
    filters.entityId = form.entityId.trim()
  }

  if (form.dateFrom) {
    filters.dateFrom = new Date(`${form.dateFrom}T00:00:00.000Z`).toISOString()
  }

  if (form.dateTo) {
    filters.dateTo = new Date(`${form.dateTo}T23:59:59.999Z`).toISOString()
  }

  emit('submit', filters)
}
</script>

<template>
  <form class="audit-log-filters-form" @submit.prevent="submitFilters">
    <BaseInput v-model="form.userId" label="User ID" placeholder="UUID" />
    <BaseInput v-model="form.action" label="Action" placeholder="DRAW_RUN" />
    <BaseInput v-model="form.entityType" label="Entity type" placeholder="draw" />
    <BaseInput v-model="form.entityId" label="Entity ID" placeholder="UUID" />
    <BaseInput v-model="form.dateFrom" type="date" label="Date from" />
    <BaseInput v-model="form.dateTo" type="date" label="Date to" />
    <BaseButton type="submit" :loading="loading">Apply</BaseButton>
  </form>
</template>

<style scoped>
.audit-log-filters-form {
  display: grid;
  grid-template-columns: repeat(6, minmax(130px, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

@media (max-width: 1180px) {
  .audit-log-filters-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .audit-log-filters-form {
    grid-template-columns: 1fr;
  }
}
</style>
