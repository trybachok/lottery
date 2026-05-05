<script setup lang="ts">
import { reactive } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import BaseSelect from '@/shared/ui/BaseSelect.vue'
import type {
  DrawStatus,
  TicketStatus,
} from '@/shared/api/generated/types.gen'
import type {
  DrawReportFilters,
  ReportExportFormat,
  TicketReportFilters,
} from '../api/adminReports.api'

type ReportKind = 'draws' | 'tickets'
type ReportFilters = DrawReportFilters | TicketReportFilters

const props = defineProps<{
  kind: ReportKind
  loading?: boolean
  exporting?: boolean
}>()

const emit = defineEmits<{
  submit: [filters: ReportFilters]
  export: [payload: { filters: ReportFilters; format: ReportExportFormat }]
}>()

const form = reactive({
  drawId: '',
  userId: '',
  status: '',
  dateFrom: '',
  dateTo: '',
})

const drawStatusOptions = [
  { label: 'Any status', value: '' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Scheduled', value: 'SCHEDULED' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Sales closed', value: 'SALES_CLOSED' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

const ticketStatusOptions = [
  { label: 'Any status', value: '' },
  { label: 'Created', value: 'CREATED' },
  { label: 'Payment pending', value: 'PAYMENT_PENDING' },
  { label: 'Paid', value: 'PAID' },
  { label: 'Participated', value: 'PARTICIPATED' },
  { label: 'Win', value: 'WIN' },
  { label: 'Lose', value: 'LOSE' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

function submitFilters(): void {
  emit('submit', buildFilters())
}

function exportFilters(format: ReportExportFormat): void {
  emit('export', {
    filters: buildFilters(),
    format,
  })
}

function buildFilters(): ReportFilters {
  const commonFilters: Omit<DrawReportFilters & TicketReportFilters, 'status'> = {
    limit: 50,
    offset: 0,
  }

  if (form.drawId.trim()) {
    commonFilters.drawId = form.drawId.trim()
  }

  if (form.userId.trim()) {
    commonFilters.userId = form.userId.trim()
  }

  if (form.dateFrom) {
    commonFilters.dateFrom = toStartOfDayIso(form.dateFrom)
  }

  if (form.dateTo) {
    commonFilters.dateTo = toEndOfDayIso(form.dateTo)
  }

  if (props.kind === 'draws') {
    return {
      ...commonFilters,
      status: form.status ? (form.status as DrawStatus) : undefined,
    } satisfies DrawReportFilters
  }

  return {
    ...commonFilters,
    status: form.status ? (form.status as TicketStatus) : undefined,
  } satisfies TicketReportFilters
}

function toStartOfDayIso(date: string): string {
  return new Date(`${date}T00:00:00.000Z`).toISOString()
}

function toEndOfDayIso(date: string): string {
  return new Date(`${date}T23:59:59.999Z`).toISOString()
}
</script>

<template>
  <form class="report-filters-form" @submit.prevent="submitFilters">
    <BaseInput v-model="form.drawId" label="Draw ID" placeholder="UUID" />
    <BaseInput v-model="form.userId" label="User ID" placeholder="UUID" />
    <BaseSelect
      v-model="form.status"
      label="Status"
      :options="kind === 'draws' ? drawStatusOptions : ticketStatusOptions"
    />
    <BaseInput v-model="form.dateFrom" type="date" label="Date from" />
    <BaseInput v-model="form.dateTo" type="date" label="Date to" />

    <div class="report-filters-form__actions">
      <BaseButton type="submit" :loading="loading">Apply</BaseButton>
      <BaseButton variant="secondary" :loading="exporting" @click="exportFilters('csv')">CSV</BaseButton>
      <BaseButton variant="secondary" :loading="exporting" @click="exportFilters('json')">JSON</BaseButton>
    </div>
  </form>
</template>

<style scoped>
.report-filters-form {
  display: grid;
  grid-template-columns: repeat(5, minmax(140px, 1fr));
  gap: 12px;
  align-items: end;
}

.report-filters-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 980px) {
  .report-filters-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .report-filters-form {
    grid-template-columns: 1fr;
  }
}
</style>
