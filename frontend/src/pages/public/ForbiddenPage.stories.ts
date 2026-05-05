import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ForbiddenPage from './ForbiddenPage.vue'

const meta: Meta<typeof ForbiddenPage> = {
  title: 'Pages/Public/ForbiddenPage',
  component: ForbiddenPage,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof ForbiddenPage>

export const Default: Story = {}
