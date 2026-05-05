import { ref } from 'vue'
import { defineStore } from 'pinia'
import { mapApiError, type FrontendApiError } from '@/shared/api/errors'
import type {
  AdminUserRequestWritable,
  Permission,
  PermissionRequest,
  Role,
  RoleRequest,
  User,
} from '@/shared/api/generated/types.gen'
import {
  assignRolePermission,
  assignUserRole,
  createAdminPermission,
  createAdminRole,
  createAdminUser,
  listAdminPermissions,
  listAdminRoles,
  listAdminUsers,
  listRolePermissions,
  listUserRoles,
} from '../api/adminRbac.api'

export const useAdminRbacStore = defineStore('admin-rbac', () => {
  const users = ref<User[]>([])
  const roles = ref<Role[]>([])
  const permissions = ref<Permission[]>([])
  const selectedUserRoles = ref<Role[]>([])
  const selectedRolePermissions = ref<Permission[]>([])
  const selectedUserId = ref<string | null>(null)
  const selectedRoleId = ref<string | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<FrontendApiError | null>(null)
  const actionError = ref<FrontendApiError | null>(null)

  async function loadAll(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      const [loadedUsers, loadedRoles, loadedPermissions] = await Promise.all([
        listAdminUsers(),
        listAdminRoles(),
        listAdminPermissions(),
      ])
      users.value = loadedUsers
      roles.value = loadedRoles
      permissions.value = loadedPermissions
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function createUser(request: AdminUserRequestWritable): Promise<User | null> {
    return saveAction(async () => {
      const user = await createAdminUser(request)
      users.value = [user, ...users.value]
      return user
    })
  }

  async function createRole(request: RoleRequest): Promise<Role | null> {
    return saveAction(async () => {
      const role = await createAdminRole(request)
      roles.value = [role, ...roles.value]
      return role
    })
  }

  async function createPermission(request: PermissionRequest): Promise<Permission | null> {
    return saveAction(async () => {
      const permission = await createAdminPermission(request)
      permissions.value = [permission, ...permissions.value]
      return permission
    })
  }

  async function selectUser(userId: string): Promise<void> {
    selectedUserId.value = userId
    actionError.value = null

    try {
      selectedUserRoles.value = await listUserRoles(userId)
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
    }
  }

  async function assignRoleToSelectedUser(roleId: string): Promise<void> {
    if (!selectedUserId.value) {
      return
    }

    await saveAction(async () => {
      await assignUserRole(selectedUserId.value!, roleId)
      selectedUserRoles.value = await listUserRoles(selectedUserId.value!)
      return null
    })
  }

  async function selectRole(roleId: string): Promise<void> {
    selectedRoleId.value = roleId
    actionError.value = null

    try {
      selectedRolePermissions.value = await listRolePermissions(roleId)
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
    }
  }

  async function assignPermissionToSelectedRole(permissionId: string): Promise<void> {
    if (!selectedRoleId.value) {
      return
    }

    await saveAction(async () => {
      await assignRolePermission(selectedRoleId.value!, permissionId)
      selectedRolePermissions.value = await listRolePermissions(selectedRoleId.value!)
      return null
    })
  }

  async function saveAction<T>(action: () => Promise<T>): Promise<T | null> {
    isSaving.value = true
    actionError.value = null

    try {
      return await action()
    } catch (caughtError) {
      actionError.value = mapApiError(caughtError)
      return null
    } finally {
      isSaving.value = false
    }
  }

  return {
    users,
    roles,
    permissions,
    selectedUserRoles,
    selectedRolePermissions,
    selectedUserId,
    selectedRoleId,
    isLoading,
    isSaving,
    error,
    actionError,
    loadAll,
    createUser,
    createRole,
    createPermission,
    selectUser,
    assignRoleToSelectedUser,
    selectRole,
    assignPermissionToSelectedRole,
  }
})
