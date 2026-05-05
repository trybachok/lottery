import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminRoleCreateForm from './AdminRoleCreateForm.vue'

const meta: Meta<typeof AdminRoleCreateForm> = {
  title: 'Features/AdminRBAC/AdminRoleCreateForm',
  component: AdminRoleCreateForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AdminRoleCreateForm>

export const Default: Story = { args: { loading: false } }
export const WithError: Story = { args: { errorMessage: 'Role code already exists.' } }
