import type { Meta, StoryObj } from '@storybook/vue3-vite'
import DrawDetailsCard from './DrawDetailsCard.vue'
import type { Draw } from '@/shared/api/generated/types.gen'

const meta: Meta<typeof DrawDetailsCard> = {
  title: 'Features/Draws/DrawDetailsCard',
  component: DrawDetailsCard,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof DrawDetailsCard>

const draw: Draw = {
  id: 'draw-1',
  title: 'Evening draw',
  description: 'Client draw with a published schedule.',
  status: 'ACTIVE',
  combinationSchemaId: 'schema-1',
  salesStartAt: '2026-05-05T12:00:00Z',
  salesEndAt: '2026-05-05T19:00:00Z',
  drawAt: '2026-05-05T20:00:00Z',
  maxTickets: 1000,
  test: false,
  createdAt: '2026-05-05T06:00:00Z',
  version: 1,
}

export const Default: Story = {
  args: {
    draw,
  },
}
