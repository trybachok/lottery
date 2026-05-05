import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminDrawReportTable from './AdminDrawReportTable.vue'
import AdminTicketReportTable from './AdminTicketReportTable.vue'
import type { Draw, Ticket } from '@/shared/api/generated/types.gen'

const meta: Meta = {
  title: 'Features/AdminReports/Tables',
  tags: ['autodocs'],
}

export default meta

const draws: Draw[] = [
  {
    id: 'draw-1',
    title: 'Morning draw',
    description: 'Daily draw',
    status: 'COMPLETED',
    managerId: 'manager-1',
    combinationSchemaId: 'schema-1',
    salesStartAt: '2026-05-05T06:00:00Z',
    salesEndAt: '2026-05-05T14:00:00Z',
    drawAt: '2026-05-05T15:00:00Z',
    maxTickets: 500,
    test: false,
    createdAt: '2026-05-05T06:00:00Z',
    version: 2,
  },
]

const tickets: Ticket[] = [
  {
    id: 'ticket-1',
    userId: 'user-1',
    drawId: 'draw-1',
    status: 'PAID',
    combinationValues: ['7', '11', '19'],
    priceAmount: '100.00',
    priceCurrency: 'RUB',
    test: false,
    createdAt: '2026-05-05T08:15:00Z',
    version: 1,
  },
  {
    id: 'ticket-2',
    userId: 'user-2',
    drawId: 'draw-1',
    status: 'WIN',
    combinationValues: ['3', '14', '27'],
    priceAmount: '100.00',
    priceCurrency: 'RUB',
    test: false,
    createdAt: '2026-05-05T08:20:00Z',
    version: 2,
  },
]

export const DrawReport: StoryObj<typeof AdminDrawReportTable> = {
  render: () => ({
    components: { AdminDrawReportTable },
    setup() {
      return { draws }
    },
    template: '<AdminDrawReportTable :draws="draws" />',
  }),
}

export const TicketReport: StoryObj<typeof AdminTicketReportTable> = {
  render: () => ({
    components: { AdminTicketReportTable },
    setup() {
      return { tickets }
    },
    template: '<AdminTicketReportTable :tickets="tickets" />',
  }),
}

export const EmptyDrawReport: StoryObj<typeof AdminDrawReportTable> = {
  render: () => ({
    components: { AdminDrawReportTable },
    template: '<AdminDrawReportTable :draws="[]" />',
  }),
}
