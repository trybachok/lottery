import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AuditLogFiltersForm from './AuditLogFiltersForm.vue'

const meta: Meta<typeof AuditLogFiltersForm> = {
  title: 'Features/AdminReports/AuditLogFiltersForm',
  component: AuditLogFiltersForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AuditLogFiltersForm>

export const Default: Story = {
  args: {
    loading: false,
  },
}

export const Loading: Story = {
  args: {
    loading: true,
  },
}
