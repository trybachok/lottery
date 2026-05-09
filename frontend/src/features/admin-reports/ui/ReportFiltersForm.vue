<script setup lang="ts">
import { reactive, ref } from 'vue'
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
  limit: '50',
})
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

const drawStatusOptions = [
  { label: 'Any status', value: '' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Scheduled', value: 'SCHEDULED' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Paused', value: 'PAUSED' },
  { label: 'Postponed', value: 'POSTPONED' },
  { label: 'Sales closed', value: 'SALES_CLOSED' },
  { label: 'Drawing', value: 'DRAWING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' },
  { label: 'Test', value: 'TEST' },
  { label: 'Archived', value: 'ARCHIVED' },
]

const ticketStatusOptions = [
  { label: 'Any status', value: '' },
  { label: 'Created', value: 'CREATED' },
  { label: 'Payment pending', value: 'PAYMENT_PENDING' },
  { label: 'Paid', value: 'PAID' },
  { label: 'Payment failed', value: 'PAYMENT_FAILED' },
  { label: 'Refund pending', value: 'REFUND_PENDING' },
  { label: 'Refunded', value: 'REFUNDED' },
  { label: 'Cancelled', value: 'CANCELLED' },
  { label: 'Participated', value: 'PARTICIPATED' },
  { label: 'Not participated', value: 'NOT_PARTICIPATED' },
  { label: 'Checked', value: 'CHECKED' },
  { label: 'Win', value: 'WIN' },
  { label: 'Lose', value: 'LOSE' },
  { label: 'Deleted', value: 'DELETED' },
  { label: 'Test', value: 'TEST' },
]

const limitOptions = [
  { label: '25 rows', value: '25' },
  { label: '50 rows', value: '50' },
  { label: '100 rows', value: '100' },
  { label: '250 rows', value: '250' },
]

function submitFilters(): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return
  emit('submit', buildFilters())
}

function exportFilters(format: ReportExportFormat): void {
  validationErrors.value = validate()
  if (Object.keys(validationErrors.value).length > 0) return
  emit('export', {
    filters: buildFilters(),
    format,
  })
}

function buildFilters(): ReportFilters {
  const commonFilters: Omit<DrawReportFilters & TicketReportFilters, 'status'> = {
    limit: Number(form.limit),
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

function validate(): Partial<Record<keyof typeof form, string>> {
  const errors: Partial<Record<keyof typeof form, string>> = {}
  if (form.dateFrom && form.dateTo && form.dateFrom > form.dateTo) {
    errors.dateTo = 'Date to must not be before date from.'
  }
  return errors
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
    <BaseInput v-model="form.dateFrom" type="date" label="Date from" :error="validationErrors.dateFrom" />
    <BaseInput v-model="form.dateTo" type="date" label="Date to" :error="validationErrors.dateTo" />
    <BaseSelect v-model="form.limit" label="Page size" :options="limitOptions" />

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
