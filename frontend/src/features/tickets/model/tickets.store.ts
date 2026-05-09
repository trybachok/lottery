import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { CreateTicketRequest, Invoice, Ticket } from '@/shared/api/generated/types.gen'
import {
  checkClientTicketResult,
  createClientTicket,
  createTicketInvoice,
  getClientTicket,
  getInvoice,
  getLatestTicketInvoice,
  listTickets,
} from '../api/tickets.api'

export const useTicketsStore = defineStore('tickets', () => {
  const items = ref<Ticket[]>([])
  const selectedTicket = ref<Ticket | null>(null)
  const selectedInvoice = ref<Invoice | null>(null)
  const invoicesByTicketId = ref<Record<string, Invoice>>({})
  const isLoading = ref(false)
  const isLoadingDetails = ref(false)
  const isCreating = ref(false)
  const invoiceLoadingTicketId = ref<string | null>(null)
  const isCheckingResult = ref(false)
  const error = ref<FrontendApiError | null>(null)
  const detailsError = ref<FrontendApiError | null>(null)
  const actionError = ref<FrontendApiError | null>(null)

  async function loadTickets(userId?: string): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      items.value = await listTickets({ userId, limit: 50, offset: 0 })
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function loadTicketDetails(ticketId: string): Promise<void> {
    isLoadingDetails.value = true
    detailsError.value = null
    selectedTicket.value = null
    selectedInvoice.value = null

    try {
      selectedTicket.value = await getClientTicket(ticketId)
      await refreshTicketInvoice(ticketId, true)
    } catch (caughtError) {
      detailsError.value = mapApiError(caughtError)
    } finally {
      isLoadingDetails.value = false
    }
  }

  async function createTicket(request: CreateTicketRequest): Promise<Ticket | null> {
    isCreating.value = true
    actionError.value = null

    try {
      const ticket = await createClientTicket(request)
      items.value = [ticket, ...items.value]
      return ticket
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      isCreating.value = false
    }
  }

  async function createInvoice(ticketId: string): Promise<Invoice | null> {
    invoiceLoadingTicketId.value = ticketId
    actionError.value = null

    try {
      const invoice = await createTicketInvoice(ticketId, {
        providerCode: 'mock',
        idempotencyKey: createIdempotencyKey(),
      })
      invoicesByTicketId.value = {
        ...invoicesByTicketId.value,
        [ticketId]: invoice,
      }
      if (selectedTicket.value?.id === ticketId) {
        selectedInvoice.value = invoice
      }
      return invoice
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      invoiceLoadingTicketId.value = null
    }
  }

  async function refreshTicketInvoice(ticketId: string, ignoreMissing = false): Promise<Invoice | null> {
    invoiceLoadingTicketId.value = ticketId
    actionError.value = null

    try {
      const invoice = await getLatestTicketInvoice(ticketId)
      invoicesByTicketId.value = {
        ...invoicesByTicketId.value,
        [ticketId]: invoice,
      }
      if (selectedTicket.value?.id === ticketId) {
        selectedInvoice.value = invoice
      }
      return invoice
    } catch (caughtError) {
      const apiError = mapApiError(caughtError)
      if (!ignoreMissing || apiError.status !== 404) {
        actionError.value = apiError
      }
      return null
    } finally {
      invoiceLoadingTicketId.value = null
    }
  }

  async function refreshInvoice(invoiceId: string): Promise<Invoice | null> {
    actionError.value = null

    try {
      const invoice = await getInvoice(invoiceId)
      invoicesByTicketId.value = {
        ...invoicesByTicketId.value,
        [invoice.ticketId]: invoice,
      }
      if (selectedTicket.value?.id === invoice.ticketId) {
        selectedInvoice.value = invoice
      }
      return invoice
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    }
  }

  async function checkResult(ticketId: string): Promise<Ticket | null> {
    isCheckingResult.value = true
    actionError.value = null

    try {
      const ticket = await checkClientTicketResult(ticketId)
      items.value = items.value.map((item) => (item.id === ticket.id ? ticket : item))
      if (selectedTicket.value?.id === ticket.id) {
        selectedTicket.value = ticket
      }
      return ticket
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      isCheckingResult.value = false
    }
  }

  function clearActionError(): void {
    actionError.value = null
  }

  return {
    items,
    selectedTicket,
    selectedInvoice,
    invoicesByTicketId,
    isLoading,
    isLoadingDetails,
    isCreating,
    invoiceLoadingTicketId,
    isCheckingResult,
    error,
    detailsError,
    actionError,
    loadTickets,
    loadTicketDetails,
    createTicket,
    createInvoice,
    refreshTicketInvoice,
    refreshInvoice,
    checkResult,
    clearActionError,
  }
})

function createIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `invoice-${Date.now()}-${Math.random().toString(36).slice(2)}`
}
