import type { Meta, StoryObj } from '@storybook/vue3-vite'
import DrawResultCard from './DrawResultCard.vue'
import type { DrawResult } from '@/shared/api/generated/types.gen'

const meta: Meta<typeof DrawResultCard> = {
  title: 'Features/Draws/DrawResultCard',
  component: DrawResultCard,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof DrawResultCard>

const result: DrawResult = {
  id: 'result-1',
  drawId: 'draw-1',
  winningCombinationValues: ['4', '8', '15', '16'],
  algorithmVersion: 'rng-v1',
  randomProvider: 'internal',
  proofHash: '9c5f4f01f4fd5b9d2f0e2c7e6a1d',
  generatedAt: '2026-05-05T20:00:00Z',
  requestId: 'request-1',
}

export const Published: Story = {
  args: {
    result,
  },
}

export const NotPublished: Story = {
  args: {
    result: null,
  },
}

export const Loading: Story = {
  args: {
    result: null,
    loading: true,
  },
}
