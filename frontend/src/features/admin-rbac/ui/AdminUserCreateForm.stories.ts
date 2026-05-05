import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminUserCreateForm from './AdminUserCreateForm.vue'

const meta: Meta<typeof AdminUserCreateForm> = {
  title: 'Features/AdminRBAC/AdminUserCreateForm',
  component: AdminUserCreateForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AdminUserCreateForm>

export const Default: Story = { args: { loading: false } }
export const Loading: Story = { args: { loading: true } }
export const WithError: Story = { args: { errorMessage: 'User email already exists.' } }
