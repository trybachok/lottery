import type { Meta, StoryObj } from '@storybook/vue3-vite'
import TicketCreateForm from './TicketCreateForm.vue'

const meta: Meta<typeof TicketCreateForm> = {
  title: 'Features/Tickets/TicketCreateForm',
  component: TicketCreateForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof TicketCreateForm>

export const Default: Story = {
  render: () => ({
    components: { TicketCreateForm },
    template: '<TicketCreateForm @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const Loading: Story = {
  render: () => ({
    components: { TicketCreateForm },
    template: '<TicketCreateForm loading @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const WithError: Story = {
  render: () => ({
    components: { TicketCreateForm },
    template: '<TicketCreateForm error-message="The selected draw is not active." @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}
