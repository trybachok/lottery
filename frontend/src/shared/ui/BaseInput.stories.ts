import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BaseInput from './BaseInput.vue'

const meta: Meta<typeof BaseInput> = {
  title: 'Design System/BaseInput',
  component: BaseInput,
  tags: ['autodocs'],
  args: {
    label: 'Email',
    modelValue: '',
    placeholder: 'client@example.com',
  },
}

export default meta
type Story = StoryObj<typeof BaseInput>

export const Default: Story = {
  render: (args) => ({
    components: { BaseInput },
    setup() {
      return { args }
    },
    template: '<BaseInput v-bind="args" />',
  }),
}

export const WithError: Story = {
  render: () => ({
    components: { BaseInput },
    template: '<BaseInput label="Password" type="password" model-value="" error="Password is required" />',
  }),
}
