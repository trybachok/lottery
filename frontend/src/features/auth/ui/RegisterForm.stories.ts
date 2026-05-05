import type { Meta, StoryObj } from '@storybook/vue3-vite'
import RegisterForm from './RegisterForm.vue'

const meta: Meta<typeof RegisterForm> = {
  title: 'Features/Auth/RegisterForm',
  component: RegisterForm,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof RegisterForm>

export const Default: Story = {
  render: () => ({
    components: { RegisterForm },
    template: '<RegisterForm @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const Loading: Story = {
  render: () => ({
    components: { RegisterForm },
    template: '<RegisterForm loading @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}

export const WithError: Story = {
  render: () => ({
    components: { RegisterForm },
    template: '<RegisterForm error-message="User with this email already exists." @submit="submit" />',
    setup() {
      return {
        submit: () => undefined,
      }
    },
  }),
}
