import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createMemoryHistory, createRouter } from 'vue-router'
import TicketDetailsCard from './TicketDetailsCard.vue'
import type { Ticket } from '@/shared/api/generated/types.gen'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/draws/:drawId', component: { template: '<div />' } },
  ],
})

const meta: Meta<typeof TicketDetailsCard> = {
  title: 'Features/Tickets/TicketDetailsCard',
  component: TicketDetailsCard,
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
type Story = StoryObj<typeof TicketDetailsCard>

const ticket: Ticket = {
  id: 'ticket-1',
  userId: 'user-1',
  drawId: 'draw-1',
  status: 'CHECKED',
  combinationValues: ['4', '8', '15', '16'],
  priceAmount: '150.00',
  priceCurrency: 'RUB',
  matchPercent: '75.00',
  prizeId: 'prize-1',
  test: false,
  createdAt: '2026-05-05T10:00:00Z',
  participatedAt: '2026-05-05T20:00:00Z',
  checkedAt: '2026-05-05T20:05:00Z',
  version: 3,
}

export const Checked: Story = {
  args: {
    ticket,
  },
}

export const Checking: Story = {
  args: {
    ticket,
    checking: true,
  },
}
