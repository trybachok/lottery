import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createMemoryHistory, createRouter } from 'vue-router'
import AdminSidebar from './AdminSidebar.vue'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    {
      path: '/admin',
      component: { template: '<div />' },
    },
    {
      path: '/admin/users',
      component: { template: '<div />' },
    },
  ],
})

const meta: Meta<typeof AdminSidebar> = {
  title: 'Widgets/Admin/AdminSidebar',
  component: AdminSidebar,
  tags: ['autodocs'],
  decorators: [
    (story) => ({
      components: { Story: story() },
      template: '<div style="width: 260px;"><Story /></div>',
      beforeCreate() {
        this.$.appContext.app.use(router)
      },
    }),
  ],
}

export default meta
type Story = StoryObj<typeof AdminSidebar>

export const Admin: Story = {
  args: {
    roleCodes: ['ADMIN'],
    userPermissions: [],
  },
}

export const Manager: Story = {
  args: {
    roleCodes: ['MANAGER'],
    userPermissions: ['draw.create', 'draw.run', 'report.draw.export'],
  },
}

export const NoAccess: Story = {
  args: {
    roleCodes: ['CLIENT'],
    userPermissions: [],
  },
}
