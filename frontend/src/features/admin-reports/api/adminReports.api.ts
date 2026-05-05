import {
  exportDrawReport,
  exportTicketReport,
  getAuditLogs,
  getDrawReport,
  getTicketReport,
} from '@/shared/api/generated/sdk.gen'
import type {
  AuditLog,
  Draw,
  ExportDrawReportData,
  ExportTicketReportData,
  GetAuditLogsData,
  GetDrawReportData,
  GetTicketReportData,
  Ticket,
} from '@/shared/api/generated/types.gen'

export type DrawReportFilters = NonNullable<GetDrawReportData['query']>
export type TicketReportFilters = NonNullable<GetTicketReportData['query']>
export type AuditLogFilters = NonNullable<GetAuditLogsData['query']>
export type ReportExportFormat = NonNullable<NonNullable<ExportDrawReportData['query']>['format']>

export async function loadDrawReport(filters: DrawReportFilters = {}): Promise<Draw[]> {
  const response = await getDrawReport({
    query: filters,
    throwOnError: true,
  })

  return response.data.items
}

export async function loadTicketReport(filters: TicketReportFilters = {}): Promise<Ticket[]> {
  const response = await getTicketReport({
    query: filters,
    throwOnError: true,
  })

  return response.data.items
}

export async function requestDrawReportExport(
  filters: DrawReportFilters,
  format: ReportExportFormat,
): Promise<Draw[]> {
  const response = await exportDrawReport({
    query: {
      ...filters,
      format,
    } satisfies NonNullable<ExportDrawReportData['query']>,
    throwOnError: true,
  })

  return response.data.items
}

export async function requestTicketReportExport(
  filters: TicketReportFilters,
  format: ReportExportFormat,
): Promise<Ticket[]> {
  const response = await exportTicketReport({
    query: {
      ...filters,
      format,
    } satisfies NonNullable<ExportTicketReportData['query']>,
    throwOnError: true,
  })

  return response.data.items
}

export async function loadAuditLogs(filters: AuditLogFilters = {}): Promise<AuditLog[]> {
  const response = await getAuditLogs({
    query: filters,
    throwOnError: true,
  })

  return response.data.items
}
