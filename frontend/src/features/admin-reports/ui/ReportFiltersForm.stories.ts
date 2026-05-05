import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportFiltersForm from './ReportFiltersForm.vue'

const meta: Meta<typeof ReportFiltersForm> = {
  title: 'Features/AdminReports/ReportFiltersForm',
  component: ReportFiltersForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof ReportFiltersForm>

export const Draws: Story = {
  args: {
    kind: 'draws',
    loading: false,
    exporting: false,
  },
}

export const TicketsExporting: Story = {
  args: {
    kind: 'tickets',
    loading: false,
    exporting: true,
  },
}
