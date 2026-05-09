import type { Meta, StoryObj } from '@storybook/vue3-vite'
import InvoiceStatusPanel from './InvoiceStatusPanel.vue'
import type { Invoice } from '@/shared/api/generated/types.gen'

const meta: Meta<typeof InvoiceStatusPanel> = {
  title: 'Features/Tickets/InvoiceStatusPanel',
  component: InvoiceStatusPanel,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof InvoiceStatusPanel>

const invoice: Invoice = {
  id: 'invoice-1',
  ticketId: 'ticket-1',
  userId: 'user-1',
  providerCode: 'mock',
  status: 'PENDING',
  amount: '150.00',
  currency: 'RUB',
  paymentUrl: 'https://example.com/pay/invoice-1',
  createdAt: '2026-05-05T10:01:00Z',
  expiresAt: '2026-05-05T11:01:00Z',
}

export const Pending: Story = {
  args: {
    invoice,
  },
}

export const WithoutInvoice: Story = {
  args: {
    invoice: null,
    canCreate: true,
  },
}

export const Refreshing: Story = {
  args: {
    invoice,
    loading: true,
  },
}
