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

type ReportPageState = {
  total: number
  limit: number
  offset: number
  hasMore: boolean
}

const emptyPage: ReportPageState = {
  total: 0,
  limit: 50,
  offset: 0,
  hasMore: false,
}

export const useAdminReportsStore = defineStore('admin-reports', () => {
  const draws = ref<Draw[]>([])
  const tickets = ref<Ticket[]>([])
  const auditLogs = ref<AuditLog[]>([])
  const drawPage = ref<ReportPageState>({ ...emptyPage })
  const ticketPage = ref<ReportPageState>({ ...emptyPage })
  const drawFilters = ref<DrawReportFilters>({ limit: 50, offset: 0 })
  const ticketFilters = ref<TicketReportFilters>({ limit: 50, offset: 0 })
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
      const normalizedFilters = withDefaultPagination(filters)
      const page = await loadDrawReport(normalizedFilters)
      draws.value = page.items
      drawPage.value = pageMeta(page)
      drawFilters.value = normalizedFilters
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
      const normalizedFilters = withDefaultPagination(filters)
      const page = await loadTicketReport(normalizedFilters)
      tickets.value = page.items
      ticketPage.value = pageMeta(page)
      ticketFilters.value = normalizedFilters
    } catch (caughtError) {
      ticketError.value = mapApiError(caughtError)
    } finally {
      isLoadingTickets.value = false
    }
  }

  async function exportDraws(filters: DrawReportFilters, format: ReportExportFormat): Promise<void> {
    await exportReport(async () => {
      const download = await requestDrawReportExport(withDefaultPagination(filters), format)
      lastExportMessage.value = `Draw report download started: ${download.filename}.`
    })
  }

  async function exportTickets(filters: TicketReportFilters, format: ReportExportFormat): Promise<void> {
    await exportReport(async () => {
      const download = await requestTicketReportExport(withDefaultPagination(filters), format)
      lastExportMessage.value = `Ticket report download started: ${download.filename}.`
    })
  }

  async function loadNextDrawPage(): Promise<void> {
    if (!drawPage.value.hasMore) return
    await loadDraws({ ...drawFilters.value, offset: drawPage.value.offset + drawPage.value.limit })
  }

  async function loadPreviousDrawPage(): Promise<void> {
    if (drawPage.value.offset <= 0) return
    await loadDraws({ ...drawFilters.value, offset: Math.max(0, drawPage.value.offset - drawPage.value.limit) })
  }

  async function loadNextTicketPage(): Promise<void> {
    if (!ticketPage.value.hasMore) return
    await loadTickets({ ...ticketFilters.value, offset: ticketPage.value.offset + ticketPage.value.limit })
  }

  async function loadPreviousTicketPage(): Promise<void> {
    if (ticketPage.value.offset <= 0) return
    await loadTickets({ ...ticketFilters.value, offset: Math.max(0, ticketPage.value.offset - ticketPage.value.limit) })
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

  function pageMeta(page: ReportPageState): ReportPageState {
    return {
      total: page.total,
      limit: page.limit,
      offset: page.offset,
      hasMore: page.hasMore,
    }
  }

  return {
    draws,
    tickets,
    auditLogs,
    drawPage,
    ticketPage,
    drawFilters,
    ticketFilters,
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
    loadNextDrawPage,
    loadPreviousDrawPage,
    loadNextTicketPage,
    loadPreviousTicketPage,
    loadAudit,
    clearActionFeedback,
  }
})
