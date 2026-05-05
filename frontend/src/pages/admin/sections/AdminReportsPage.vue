<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AdminDrawReportTable from '@/features/admin-reports/ui/AdminDrawReportTable.vue'
import AdminTicketReportTable from '@/features/admin-reports/ui/AdminTicketReportTable.vue'
import ReportFiltersForm from '@/features/admin-reports/ui/ReportFiltersForm.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import { useAdminReportsStore } from '@/features/admin-reports/model/adminReports.store'
import { hasPermission } from '@/shared/lib/permissions/hasPermission'
import type {
  DrawReportFilters,
  ReportExportFormat,
  TicketReportFilters,
} from '@/features/admin-reports/api/adminReports.api'

const reportsStore = useAdminReportsStore()
const authStore = useAuthStore()
type ReportFilters = DrawReportFilters | TicketReportFilters

const isAdmin = computed(() => authStore.roleCodes.includes('ADMIN'))
const canUseDrawReport = computed(
  () => isAdmin.value || hasPermission(authStore.permissions, ['report.draw.export']),
)
const canUseTicketReport = computed(
  () => isAdmin.value || hasPermission(authStore.permissions, ['report.ticket.export']),
)

onMounted(() => {
  if (canUseDrawReport.value) {
    void reportsStore.loadDraws()
  }

  if (canUseTicketReport.value) {
    void reportsStore.loadTickets()
  }
})

function applyDrawFilters(filters: ReportFilters): void {
  reportsStore.clearActionFeedback()
  void reportsStore.loadDraws(filters as DrawReportFilters)
}

function applyTicketFilters(filters: ReportFilters): void {
  reportsStore.clearActionFeedback()
  void reportsStore.loadTickets(filters as TicketReportFilters)
}

function exportDraws(payload: { filters: ReportFilters; format: ReportExportFormat }): void {
  void reportsStore.exportDraws(payload.filters as DrawReportFilters, payload.format)
}

function exportTickets(payload: { filters: ReportFilters; format: ReportExportFormat }): void {
  void reportsStore.exportTickets(payload.filters as TicketReportFilters, payload.format)
}
</script>

<template>
  <main class="admin-reports-page">
    <BaseCard
      v-if="canUseDrawReport"
      title="Draw report filters"
      description="Filter draw reports and request CSV or JSON export."
    >
      <ReportFiltersForm
        kind="draws"
        :loading="reportsStore.isLoadingDraws"
        :exporting="reportsStore.isExporting"
        @submit="applyDrawFilters"
        @export="exportDraws"
      />
    </BaseCard>
    <AppErrorMessage
      v-else
      title="Draw report unavailable"
      message="Your account can view this admin section, but it does not have report.draw.export."
    />

    <AppErrorMessage
      v-if="reportsStore.actionError"
      title="Export failed"
      :message="reportsStore.actionError.message"
    />
    <BaseCard v-else-if="reportsStore.lastExportMessage" title="Export completed">
      <p class="admin-reports-page__message">{{ reportsStore.lastExportMessage }}</p>
    </BaseCard>

    <AppLoader v-if="canUseDrawReport && reportsStore.isLoadingDraws" label="Loading draw report..." />
    <AppErrorMessage
      v-else-if="canUseDrawReport && reportsStore.drawError"
      title="Could not load draw report"
      :message="reportsStore.drawError.message"
    />
    <AdminDrawReportTable v-else-if="canUseDrawReport" :draws="reportsStore.draws" />

    <BaseCard
      v-if="canUseTicketReport"
      title="Ticket report filters"
      description="Filter ticket reports and request CSV or JSON export."
    >
      <ReportFiltersForm
        kind="tickets"
        :loading="reportsStore.isLoadingTickets"
        :exporting="reportsStore.isExporting"
        @submit="applyTicketFilters"
        @export="exportTickets"
      />
    </BaseCard>
    <AppErrorMessage
      v-else
      title="Ticket report unavailable"
      message="Your account can view this admin section, but it does not have report.ticket.export."
    />

    <AppLoader v-if="canUseTicketReport && reportsStore.isLoadingTickets" label="Loading ticket report..." />
    <AppErrorMessage
      v-else-if="canUseTicketReport && reportsStore.ticketError"
      title="Could not load ticket report"
      :message="reportsStore.ticketError.message"
    />
    <AdminTicketReportTable v-else-if="canUseTicketReport" :tickets="reportsStore.tickets" />
  </main>
</template>

<style scoped>
.admin-reports-page {
  display: grid;
  gap: 20px;
}

.admin-reports-page__message {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9375rem;
}
</style>
