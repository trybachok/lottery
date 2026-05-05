import {
  createInvoiceForTicket,
  createTicket,
  getTickets,
} from '@/shared/api/generated/sdk.gen'
import type {
  CreateInvoiceRequest,
  CreateTicketRequest,
  Invoice,
  Ticket,
} from '@/shared/api/generated/types.gen'

export async function listTickets(params: {
  userId?: string
  limit?: number
  offset?: number
} = {}): Promise<Ticket[]> {
  const response = await getTickets({
    query: params,
    throwOnError: true,
  })

  return response.data.items
}

export async function createClientTicket(request: CreateTicketRequest): Promise<Ticket> {
  const response = await createTicket({
    body: request,
    throwOnError: true,
  })

  return response.data
}

export async function createTicketInvoice(ticketId: string, request: CreateInvoiceRequest): Promise<Invoice> {
  const response = await createInvoiceForTicket({
    path: {
      ticketId,
    },
    body: request,
    throwOnError: true,
  })

  return response.data
}
