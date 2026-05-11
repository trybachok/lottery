import {
  checkTicketResult,
  createInvoiceForTicket,
  createTicket,
  getInvoiceById,
  getTicketById,
  getTicketInvoice,
  getTickets,
  simulateMockPaymentWebhook,
} from '@/shared/api/generated/sdk.gen'
import type {
  CreateInvoiceRequest,
  CreateTicketRequest,
  Invoice,
  MockPaymentWebhookRequest,
  PaymentWebhookResult,
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

export async function getClientTicket(ticketId: string): Promise<Ticket> {
  const response = await getTicketById({
    path: { ticketId },
    throwOnError: true,
  })

  return response.data
}

export async function getLatestTicketInvoice(ticketId: string): Promise<Invoice> {
  const response = await getTicketInvoice({
    path: { ticketId },
    throwOnError: true,
  })

  return response.data
}

export async function getInvoice(invoiceId: string): Promise<Invoice> {
  const response = await getInvoiceById({
    path: { invoiceId },
    throwOnError: true,
  })

  return response.data
}

export async function sendMockPaymentWebhook(
  invoiceId: string,
  eventType: MockPaymentWebhookRequest['eventType'],
): Promise<PaymentWebhookResult> {
  const response = await simulateMockPaymentWebhook({
    path: { invoiceId },
    body: { eventType },
    throwOnError: true,
  })

  return response.data
}

export async function checkClientTicketResult(ticketId: string): Promise<Ticket> {
  const response = await checkTicketResult({
    path: { ticketId },
    throwOnError: true,
  })

  return response.data
}
