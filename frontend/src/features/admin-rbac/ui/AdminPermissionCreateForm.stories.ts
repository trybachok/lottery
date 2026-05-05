import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminPermissionCreateForm from './AdminPermissionCreateForm.vue'

const meta: Meta<typeof AdminPermissionCreateForm> = {
  title: 'Features/AdminRBAC/AdminPermissionCreateForm',
  component: AdminPermissionCreateForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AdminPermissionCreateForm>

export const Default: Story = { args: { loading: false } }
export const WithError: Story = { args: { errorMessage: 'Permission code already exists.' } }
