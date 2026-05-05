import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminDrawCreateForm from './AdminDrawCreateForm.vue'

const meta: Meta<typeof AdminDrawCreateForm> = {
  title: 'Features/AdminDraws/AdminDrawCreateForm',
  component: AdminDrawCreateForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AdminDrawCreateForm>

export const Default: Story = {
  render: () => ({
    components: { AdminDrawCreateForm },
    template: '<AdminDrawCreateForm @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const Loading: Story = {
  render: () => ({
    components: { AdminDrawCreateForm },
    template: '<AdminDrawCreateForm loading @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const WithError: Story = {
  render: () => ({
    components: { AdminDrawCreateForm },
    template: '<AdminDrawCreateForm error-message="Combination schema was not found." @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}
