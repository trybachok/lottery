import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BaseButton from '@/shared/ui/BaseButton.vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import PermissionGate from './PermissionGate.vue'

const meta: Meta<typeof PermissionGate> = {
  title: 'Shared/Permissions/PermissionGate',
  component: PermissionGate,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof PermissionGate>

export const Allowed: Story = {
  render: () => ({
    components: { BaseButton, PermissionGate },
    template: `
      <PermissionGate :user-permissions="['draw.run']" :permissions="['draw.run']">
        <BaseButton>Run draw</BaseButton>
      </PermissionGate>
    `,
  }),
}

export const Denied: Story = {
  render: () => ({
    components: { AppErrorMessage, BaseButton, PermissionGate },
    template: `
      <PermissionGate :user-permissions="['ticket.read']" :permissions="['draw.run']">
        <BaseButton>Run draw</BaseButton>
        <template #fallback>
          <AppErrorMessage message="You do not have permission to run this draw." />
        </template>
      </PermissionGate>
    `,
  }),
}
