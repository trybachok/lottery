import {
  exportDrawReport,
  exportTicketReport,
  getAuditLogs,
  getDrawReport,
  getTicketReport,
} from '@/shared/api/generated/sdk.gen'
import type {
  AuditLog,
  DrawReportResponse,
  ExportDrawReportData,
  ExportTicketReportData,
  GetAuditLogsData,
  GetDrawReportData,
  GetTicketReportData,
  TicketReportResponse,
} from '@/shared/api/generated/types.gen'

export type DrawReportFilters = NonNullable<GetDrawReportData['query']>
export type TicketReportFilters = NonNullable<GetTicketReportData['query']>
export type AuditLogFilters = NonNullable<GetAuditLogsData['query']>
export type ReportExportFormat = NonNullable<NonNullable<ExportDrawReportData['query']>['format']>
export type ReportDownload = {
  filename: string
}

export async function loadDrawReport(filters: DrawReportFilters = {}): Promise<DrawReportResponse> {
  const response = await getDrawReport({
    query: filters,
    throwOnError: true,
  })

  return response.data
}

export async function loadTicketReport(filters: TicketReportFilters = {}): Promise<TicketReportResponse> {
  const response = await getTicketReport({
    query: filters,
    throwOnError: true,
  })

  return response.data
}

export async function requestDrawReportExport(
  filters: DrawReportFilters,
  format: ReportExportFormat,
): Promise<ReportDownload> {
  const response = await exportDrawReport({
    query: {
      ...filters,
      format,
    } satisfies NonNullable<ExportDrawReportData['query']>,
    responseType: 'blob',
    throwOnError: true,
  })

  const blob = response.data as unknown as Blob
  const filename = filenameFromDisposition(response.headers['content-disposition']) ?? `draw-report.${format}`
  downloadBlob(blob, filename)
  return { filename }
}

export async function requestTicketReportExport(
  filters: TicketReportFilters,
  format: ReportExportFormat,
): Promise<ReportDownload> {
  const response = await exportTicketReport({
    query: {
      ...filters,
      format,
    } satisfies NonNullable<ExportTicketReportData['query']>,
    responseType: 'blob',
    throwOnError: true,
  })

  const blob = response.data as unknown as Blob
  const filename = filenameFromDisposition(response.headers['content-disposition']) ?? `ticket-report.${format}`
  downloadBlob(blob, filename)
  return { filename }
}

export async function loadAuditLogs(filters: AuditLogFilters = {}): Promise<AuditLog[]> {
  const response = await getAuditLogs({
    query: filters,
    throwOnError: true,
  })

  return response.data.items
}

function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.style.display = 'none'
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 0)
}

function filenameFromDisposition(disposition: string | undefined): string | null {
  if (!disposition) {
    return null
  }
  const filenameMatch = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition)
  return filenameMatch?.[1] ? decodeURIComponent(filenameMatch[1]) : null
}
