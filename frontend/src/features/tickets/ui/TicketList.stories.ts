import type { Meta, StoryObj } from '@storybook/vue3-vite'
import TicketList from './TicketList.vue'
import type { Invoice, Ticket } from '@/shared/api/generated/types.gen'

const meta: Meta<typeof TicketList> = {
  title: 'Features/Tickets/TicketList',
  component: TicketList,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof TicketList>

const tickets: Ticket[] = [
  {
    id: 'ticket-1',
    userId: 'user-1',
    drawId: 'draw-1',
    status: 'CREATED',
    combinationValues: ['12', '18', '27', '34'],
    priceAmount: '100.00',
    priceCurrency: 'RUB',
    test: false,
    createdAt: '2026-05-05T09:00:00Z',
    version: 1,
  },
  {
    id: 'ticket-2',
    userId: 'user-1',
    drawId: 'draw-2',
    status: 'PAYMENT_PENDING',
    combinationValues: ['4', '8', '15', '16'],
    priceAmount: '150.00',
    priceCurrency: 'RUB',
    test: false,
    createdAt: '2026-05-05T10:00:00Z',
    version: 1,
  },
]

const invoice: Invoice = {
  id: 'invoice-1',
  ticketId: 'ticket-2',
  userId: 'user-1',
  providerCode: 'mock',
  status: 'PENDING',
  amount: '150.00',
  currency: 'RUB',
  paymentUrl: 'https://example.com/pay/invoice-1',
  createdAt: '2026-05-05T10:01:00Z',
}

export const Default: Story = {
  args: {
    tickets,
    invoicesByTicketId: {
      'ticket-2': invoice,
    },
    invoiceLoadingTicketId: null,
  },
}

export const LoadingInvoice: Story = {
  args: {
    tickets,
    invoicesByTicketId: {},
    invoiceLoadingTicketId: 'ticket-1',
  },
}

export const Empty: Story = {
  args: {
    tickets: [],
    invoicesByTicketId: {},
    invoiceLoadingTicketId: null,
  },
}
