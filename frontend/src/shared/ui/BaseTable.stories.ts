import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BaseTable from './BaseTable.vue'

const meta: Meta = {
  title: 'Design System/BaseTable',
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof BaseTable>

const columns = [
  { key: 'title', label: 'Draw' },
  { key: 'status', label: 'Status' },
  { key: 'tickets', label: 'Tickets', align: 'right' },
] satisfies Array<{ key: string; label: string; align?: 'left' | 'right' | 'center' }>

const rows = [
  { id: '1', title: 'Morning draw', status: 'ACTIVE', tickets: 140 },
  { id: '2', title: 'Evening draw', status: 'SALES_CLOSED', tickets: 300 },
]

export const Default: Story = {
  render: () => ({
    components: { BaseTable },
    setup() {
      return { columns, rows }
    },
    template: '<BaseTable :columns="columns" :rows="rows" />',
  }),
}

export const Empty: Story = {
  render: () => ({
    components: { BaseTable },
    setup() {
      return { columns }
    },
    template: '<BaseTable :columns="columns" :rows="[]" empty-message="No draws yet" />',
  }),
}
