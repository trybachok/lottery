import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type { CreateTicketRequest, Invoice, Ticket } from '@/shared/api/generated/types.gen'
import {
  createClientTicket,
  createTicketInvoice,
  listTickets,
} from '../api/tickets.api'

export const useTicketsStore = defineStore('tickets', () => {
  const items = ref<Ticket[]>([])
  const invoicesByTicketId = ref<Record<string, Invoice>>({})
  const isLoading = ref(false)
  const isCreating = ref(false)
  const invoiceLoadingTicketId = ref<string | null>(null)
  const error = ref<FrontendApiError | null>(null)
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
      return invoice
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      invoiceLoadingTicketId.value = null
    }
  }

  function clearActionError(): void {
    actionError.value = null
  }

  return {
    items,
    invoicesByTicketId,
    isLoading,
    isCreating,
    invoiceLoadingTicketId,
    error,
    actionError,
    loadTickets,
    createTicket,
    createInvoice,
    clearActionError,
  }
})

function createIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `invoice-${Date.now()}-${Math.random().toString(36).slice(2)}`
}
