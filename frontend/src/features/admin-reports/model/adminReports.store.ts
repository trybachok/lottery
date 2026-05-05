import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { AuditLog, Draw, Ticket } from '@/shared/api/generated/types.gen'
import {
  loadAuditLogs,
  loadDrawReport,
  loadTicketReport,
  requestDrawReportExport,
  requestTicketReportExport,
  type AuditLogFilters,
  type DrawReportFilters,
  type ReportExportFormat,
  type TicketReportFilters,
} from '../api/adminReports.api'

export const useAdminReportsStore = defineStore('admin-reports', () => {
  const draws = ref<Draw[]>([])
  const tickets = ref<Ticket[]>([])
  const auditLogs = ref<AuditLog[]>([])
  const isLoadingDraws = ref(false)
  const isLoadingTickets = ref(false)
  const isLoadingAuditLogs = ref(false)
  const isExporting = ref(false)
  const drawError = ref<FrontendApiError | null>(null)
  const ticketError = ref<FrontendApiError | null>(null)
  const auditError = ref<FrontendApiError | null>(null)
  const actionError = ref<FrontendApiError | null>(null)
  const lastExportMessage = ref<string | null>(null)

  async function loadDraws(filters: DrawReportFilters = {}): Promise<void> {
    isLoadingDraws.value = true
    drawError.value = null

    try {
      draws.value = await loadDrawReport(withDefaultPagination(filters))
    } catch (caughtError) {
      drawError.value = mapApiError(caughtError)
    } finally {
      isLoadingDraws.value = false
    }
  }

  async function loadTickets(filters: TicketReportFilters = {}): Promise<void> {
    isLoadingTickets.value = true
    ticketError.value = null

    try {
      tickets.value = await loadTicketReport(withDefaultPagination(filters))
    } catch (caughtError) {
      ticketError.value = mapApiError(caughtError)
    } finally {
      isLoadingTickets.value = false
    }
  }

  async function exportDraws(filters: DrawReportFilters, format: ReportExportFormat): Promise<void> {
    await exportReport(async () => {
      const exportedDraws = await requestDrawReportExport(withDefaultPagination(filters), format)
      lastExportMessage.value = `Draw report export completed: ${exportedDraws.length} records in ${format.toUpperCase()} format.`
    })
  }

  async function exportTickets(filters: TicketReportFilters, format: ReportExportFormat): Promise<void> {
    await exportReport(async () => {
      const exportedTickets = await requestTicketReportExport(withDefaultPagination(filters), format)
      lastExportMessage.value = `Ticket report export completed: ${exportedTickets.length} records in ${format.toUpperCase()} format.`
    })
  }

  async function loadAudit(filters: AuditLogFilters = {}): Promise<void> {
    isLoadingAuditLogs.value = true
    auditError.value = null

    try {
      auditLogs.value = await loadAuditLogs(withDefaultPagination(filters))
    } catch (caughtError) {
      auditError.value = mapApiError(caughtError)
    } finally {
      isLoadingAuditLogs.value = false
    }
  }

  async function exportReport(action: () => Promise<void>): Promise<void> {
    isExporting.value = true
    actionError.value = null
    lastExportMessage.value = null

    try {
      await action()
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
    } finally {
      isExporting.value = false
    }
  }

  function clearActionFeedback(): void {
    actionError.value = null
    lastExportMessage.value = null
  }

  function withDefaultPagination<TFilters extends { limit?: number; offset?: number }>(filters: TFilters): TFilters {
    return {
      limit: 50,
      offset: 0,
      ...filters,
    }
  }

  return {
    draws,
    tickets,
    auditLogs,
    isLoadingDraws,
    isLoadingTickets,
    isLoadingAuditLogs,
    isExporting,
    drawError,
    ticketError,
    auditError,
    actionError,
    lastExportMessage,
    loadDraws,
    loadTickets,
    exportDraws,
    exportTickets,
    loadAudit,
    clearActionFeedback,
  }
})
