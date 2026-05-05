import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppErrorMessage from './AppErrorMessage.vue'
import AppLoader from './AppLoader.vue'

const meta: Meta = {
  title: 'Design System/Feedback',
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj

export const ErrorMessage: Story = {
  render: () => ({
    components: { AppErrorMessage },
    template: '<AppErrorMessage title="Could not load tickets" message="Please try again later." />',
  }),
}

export const Loader: Story = {
  render: () => ({
    components: { AppLoader },
    template: '<AppLoader label="Loading draws..." />',
  }),
}
