import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BaseButton from './BaseButton.vue'
import BaseCard from './BaseCard.vue'

const meta: Meta<typeof BaseCard> = {
  title: 'Design System/BaseCard',
  component: BaseCard,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof BaseCard>

export const Default: Story = {
  render: () => ({
    components: { BaseButton, BaseCard },
    template: `
      <BaseCard title="Morning draw" description="Sales close today at 18:00.">
        <template #actions>
          <BaseButton size="sm">Open</BaseButton>
        </template>
        <p style="margin: 0; color: var(--color-text-muted);">Prize pool: 120000 RUB</p>
      </BaseCard>
    `,
  }),
}
