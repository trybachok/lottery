import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BaseButton from './BaseButton.vue'

const meta: Meta<typeof BaseButton> = {
  title: 'Design System/BaseButton',
  component: BaseButton,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'danger', 'ghost'],
    },
    size: {
      control: 'select',
      options: ['sm', 'md'],
    },
  },
  args: {
    variant: 'primary',
    size: 'md',
    loading: false,
    disabled: false,
  },
}

export default meta
type Story = StoryObj<typeof BaseButton>

export const Primary: Story = {
  render: (args) => ({
    components: { BaseButton },
    setup() {
      return { args }
    },
    template: '<BaseButton v-bind="args">Create ticket</BaseButton>',
  }),
}

export const Variants: Story = {
  render: () => ({
    components: { BaseButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center;">
        <BaseButton>Primary</BaseButton>
        <BaseButton variant="secondary">Secondary</BaseButton>
        <BaseButton variant="danger">Danger</BaseButton>
        <BaseButton variant="ghost">Ghost</BaseButton>
      </div>
    `,
  }),
}

export const Loading: Story = {
  render: () => ({
    components: { BaseButton },
    template: '<BaseButton loading>Saving</BaseButton>',
  }),
}
