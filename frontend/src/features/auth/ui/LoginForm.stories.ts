import type { Meta, StoryObj } from '@storybook/vue3-vite'
import LoginForm from './LoginForm.vue'

const meta: Meta<typeof LoginForm> = {
  title: 'Features/Auth/LoginForm',
  component: LoginForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof LoginForm>

export const Default: Story = {
  render: () => ({
    components: { LoginForm },
    template: '<LoginForm @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const Loading: Story = {
  render: () => ({
    components: { LoginForm },
    template: '<LoginForm loading @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const WithError: Story = {
  render: () => ({
    components: { LoginForm },
    template: '<LoginForm error-message="Invalid login or password." @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}
