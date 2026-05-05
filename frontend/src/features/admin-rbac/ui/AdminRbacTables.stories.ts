import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminPermissionsTable from './AdminPermissionsTable.vue'
import AdminRolesTable from './AdminRolesTable.vue'
import AdminUsersTable from './AdminUsersTable.vue'
import RoleAssignmentPanel from './RoleAssignmentPanel.vue'
import PermissionAssignmentPanel from './PermissionAssignmentPanel.vue'
import type { Permission, Role, User } from '@/shared/api/generated/types.gen'

const meta: Meta = {
  title: 'Features/AdminRBAC/TablesAndAssignments',
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj

const users: User[] = [
  {
    id: 'user-1',
    email: 'admin@example.com',
    login: 'admin',
    status: 'ACTIVE',
    createdAt: '2026-05-05T09:00:00Z',
    version: 1,
  },
]

const roles: Role[] = [
  { id: 'role-1', code: 'ADMIN', name: 'Administrator', system: true },
  { id: 'role-2', code: 'MANAGER', name: 'Manager', system: true },
]

const permissions: Permission[] = [
  { id: 'permission-1', code: 'user.manage', description: 'Manage users' },
  { id: 'permission-2', code: 'draw.run', description: 'Run draws' },
]

export const Users: Story = {
  render: () => ({
    components: { AdminUsersTable, RoleAssignmentPanel },
    setup() {
      return { users, roles }
    },
    template: `
      <div style="display: grid; gap: 16px; width: 720px;">
        <AdminUsersTable :users="users" selected-user-id="user-1" />
        <RoleAssignmentPanel selected-user-id="user-1" :user-roles="roles" />
      </div>
    `,
  }),
}

export const Roles: Story = {
  render: () => ({
    components: { AdminRolesTable, PermissionAssignmentPanel },
    setup() {
      return { roles, permissions }
    },
    template: `
      <div style="display: grid; gap: 16px; width: 720px;">
        <AdminRolesTable :roles="roles" selected-role-id="role-1" />
        <PermissionAssignmentPanel selected-role-id="role-1" :role-permissions="permissions" />
      </div>
    `,
  }),
}

export const Permissions: Story = {
  render: () => ({
    components: { AdminPermissionsTable },
    setup() {
      return { permissions }
    },
    template: '<div style="width: 720px;"><AdminPermissionsTable :permissions="permissions" /></div>',
  }),
}
