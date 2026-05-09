import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createMemoryHistory, createRouter } from 'vue-router'
import DrawList from './DrawList.vue'
import type { Draw } from '@/shared/api/generated/types.gen'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/draws/:drawId', component: { template: '<div />' } },
  ],
})

const meta: Meta<typeof DrawList> = {
  title: 'Features/Draws/DrawList',
  component: DrawList,
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
type Story = StoryObj<typeof DrawList>

const draws: Draw[] = [
  {
    id: 'draw-1',
    title: 'Morning draw',
    description: 'Daily client draw',
    status: 'ACTIVE',
    combinationSchemaId: 'schema-1',
    salesStartAt: '2026-05-05T06:00:00Z',
    salesEndAt: '2026-05-05T15:00:00Z',
    drawAt: '2026-05-05T16:00:00Z',
    maxTickets: 500,
    test: false,
    createdAt: '2026-05-05T06:00:00Z',
    version: 1,
  },
  {
    id: 'draw-2',
    title: 'Evening draw',
    description: 'Evening client draw',
    status: 'SCHEDULED',
    combinationSchemaId: 'schema-1',
    salesStartAt: '2026-05-05T12:00:00Z',
    salesEndAt: '2026-05-05T19:00:00Z',
    drawAt: '2026-05-05T20:00:00Z',
    test: false,
    createdAt: '2026-05-05T06:00:00Z',
    version: 1,
  },
]

export const Default: Story = {
  args: {
    draws,
  },
}

export const Empty: Story = {
  args: {
    draws: [],
  },
}
