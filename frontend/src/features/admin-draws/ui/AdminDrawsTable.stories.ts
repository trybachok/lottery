import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminDrawsTable from './AdminDrawsTable.vue'
import type { Draw } from '@/shared/api/generated/types.gen'

const meta: Meta<typeof AdminDrawsTable> = {
  title: 'Features/AdminDraws/AdminDrawsTable',
  component: AdminDrawsTable,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AdminDrawsTable>

const draws: Draw[] = [
  {
    id: 'draw-1',
    title: 'Morning draw',
    description: 'Daily draw',
    status: 'SALES_CLOSED',
    managerId: 'manager-1',
    combinationSchemaId: 'schema-1',
    salesStartAt: '2026-05-05T06:00:00Z',
    salesEndAt: '2026-05-05T14:00:00Z',
    drawAt: '2026-05-05T15:00:00Z',
    maxTickets: 500,
    test: false,
    createdAt: '2026-05-05T06:00:00Z',
    version: 1,
  },
  {
    id: 'draw-2',
    title: 'Evening draw',
    description: 'Evening draw',
    status: 'ACTIVE',
    combinationSchemaId: 'schema-1',
    salesStartAt: '2026-05-05T12:00:00Z',
    salesEndAt: '2026-05-05T18:00:00Z',
    drawAt: '2026-05-05T19:00:00Z',
    test: false,
    createdAt: '2026-05-05T06:00:00Z',
    version: 1,
  },
]

export const Default: Story = {
  args: {
    draws,
    runningDrawId: null,
    assigningManagerDrawId: null,
    canRun: true,
    canAssignManager: true,
  },
}

export const Running: Story = {
  args: {
    draws,
    runningDrawId: 'draw-1',
    assigningManagerDrawId: null,
    canRun: true,
    canAssignManager: true,
  },
}

export const ReadOnly: Story = {
  args: {
    draws,
    runningDrawId: null,
    assigningManagerDrawId: null,
    canRun: false,
    canAssignManager: false,
  },
}

export const Empty: Story = {
  args: {
    draws: [],
    runningDrawId: null,
    assigningManagerDrawId: null,
    canRun: true,
    canAssignManager: true,
  },
}
