import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createMemoryHistory, createRouter } from 'vue-router'
import ResultHistory from './ResultHistory.vue'
import type { Ticket } from '@/shared/api/generated/types.gen'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/draws/:drawId', component: { template: '<div />' } },
    { path: '/account/tickets/:ticketId', component: { template: '<div />' } },
  ],
})

const meta: Meta<typeof ResultHistory> = {
  title: 'Features/Tickets/ResultHistory',
  component: ResultHistory,
  tags: ['autodocs'],
  decorators: [
    (story) => ({
      components: { Story: story() },
      template: '<Story />',
      beforeCreate() {
        this.$.appContext.app.use(router)
      },
    }),
  ],
}

export default meta
type Story = StoryObj<typeof ResultHistory>

const tickets: Ticket[] = [
  {
    id: 'ticket-1',
    userId: 'user-1',
    drawId: 'draw-1',
    status: 'WIN',
    combinationValues: ['4', '8', '15', '16'],
    priceAmount: '150.00',
    priceCurrency: 'RUB',
    matchPercent: '100.00',
    prizeId: 'prize-1',
    test: false,
    createdAt: '2026-05-05T10:00:00Z',
    checkedAt: '2026-05-05T20:05:00Z',
    version: 3,
  },
  {
    id: 'ticket-2',
    userId: 'user-1',
    drawId: 'draw-2',
    status: 'CREATED',
    combinationValues: ['1', '2', '3', '4'],
    priceAmount: '100.00',
    priceCurrency: 'RUB',
    test: false,
    createdAt: '2026-05-05T11:00:00Z',
    version: 1,
  },
]

export const Default: Story = {
  args: {
    tickets,
  },
}

export const Empty: Story = {
  args: {
    tickets: [],
  },
}
