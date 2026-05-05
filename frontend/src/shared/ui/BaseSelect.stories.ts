import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BaseSelect from './BaseSelect.vue'

const meta: Meta<typeof BaseSelect> = {
  title: 'Design System/BaseSelect',
  component: BaseSelect,
  tags: ['autodocs'],
  args: {
    label: 'Status',
    modelValue: '',
    options: [
      { label: 'Any status', value: '' },
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Completed', value: 'COMPLETED' },
    ],
  },
}

export default meta
type Story = StoryObj<typeof BaseSelect>

export const Default: Story = {
  render: (args) => ({
    components: { BaseSelect },
    setup() {
      return { args }
    },
    template: '<BaseSelect v-bind="args" />',
  }),
}

export const WithError: Story = {
  render: () => ({
    components: { BaseSelect },
    template:
      '<BaseSelect label="Format" model-value="" error="Format is required" :options="[{ label: \'Select format\', value: \'\' }, { label: \'CSV\', value: \'csv\' }]" />',
  }),
}
