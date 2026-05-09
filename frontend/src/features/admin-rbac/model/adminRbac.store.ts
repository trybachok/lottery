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
  deleteAdminPermission,
  deleteAdminRole,
  deleteAdminUser,
  listAdminPermissions,
  listAdminRoles,
  listAdminUsers,
  listRolePermissions,
  listUserRoles,
  removeRolePermission,
  removeUserRole,
  updateAdminPermission,
  updateAdminRole,
  updateAdminUser,
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

  async function loadUsers(): Promise<void> {
    await loadResource(async () => {
      users.value = await listAdminUsers()
    })
  }

  async function loadUsersAndRoles(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      const [loadedUsers, loadedRoles] = await Promise.all([listAdminUsers(), listAdminRoles()])
      users.value = loadedUsers
      roles.value = loadedRoles
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function loadRoles(): Promise<void> {
    await loadResource(async () => {
      roles.value = await listAdminRoles()
    })
  }

  async function loadRolesAndPermissions(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      const [loadedRoles, loadedPermissions] = await Promise.all([listAdminRoles(), listAdminPermissions()])
      roles.value = loadedRoles
      permissions.value = loadedPermissions
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
    }
  }

  async function loadPermissions(): Promise<void> {
    await loadResource(async () => {
      permissions.value = await listAdminPermissions()
    })
  }

  async function createUser(request: AdminUserRequestWritable): Promise<User | null> {
    return saveAction(async () => {
      const user = await createAdminUser(request)
      users.value = [user, ...users.value]
      return user
    })
  }

  async function updateUser(userId: string, request: AdminUserRequestWritable): Promise<User | null> {
    return saveAction(async () => {
      const user = await updateAdminUser(userId, request)
      users.value = users.value.map((item) => (item.id === user.id ? user : item))
      return user
    })
  }

  async function deleteUser(userId: string): Promise<void> {
    await saveAction(async () => {
      await deleteAdminUser(userId)
      users.value = users.value.filter((user) => user.id !== userId)
      if (selectedUserId.value === userId) {
        selectedUserId.value = null
        selectedUserRoles.value = []
      }
      return null
    })
  }

  async function createRole(request: RoleRequest): Promise<Role | null> {
    return saveAction(async () => {
      const role = await createAdminRole(request)
      roles.value = [role, ...roles.value]
      return role
    })
  }

  async function updateRole(roleId: string, request: RoleRequest): Promise<Role | null> {
    return saveAction(async () => {
      const role = await updateAdminRole(roleId, request)
      roles.value = roles.value.map((item) => (item.id === role.id ? role : item))
      selectedUserRoles.value = selectedUserRoles.value.map((item) => (item.id === role.id ? role : item))
      return role
    })
  }

  async function deleteRole(roleId: string): Promise<void> {
    await saveAction(async () => {
      await deleteAdminRole(roleId)
      roles.value = roles.value.filter((role) => role.id !== roleId)
      selectedUserRoles.value = selectedUserRoles.value.filter((role) => role.id !== roleId)
      if (selectedRoleId.value === roleId) {
        selectedRoleId.value = null
        selectedRolePermissions.value = []
      }
      return null
    })
  }

  async function createPermission(request: PermissionRequest): Promise<Permission | null> {
    return saveAction(async () => {
      const permission = await createAdminPermission(request)
      permissions.value = [permission, ...permissions.value]
      return permission
    })
  }

  async function updatePermission(permissionId: string, request: PermissionRequest): Promise<Permission | null> {
    return saveAction(async () => {
      const permission = await updateAdminPermission(permissionId, request)
      permissions.value = permissions.value.map((item) => (item.id === permission.id ? permission : item))
      selectedRolePermissions.value = selectedRolePermissions.value.map((item) =>
        item.id === permission.id ? permission : item,
      )
      return permission
    })
  }

  async function deletePermission(permissionId: string): Promise<void> {
    await saveAction(async () => {
      await deleteAdminPermission(permissionId)
      permissions.value = permissions.value.filter((permission) => permission.id !== permissionId)
      selectedRolePermissions.value = selectedRolePermissions.value.filter((permission) => permission.id !== permissionId)
      return null
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

  async function removeRoleFromSelectedUser(roleId: string): Promise<void> {
    if (!selectedUserId.value) {
      return
    }

    await saveAction(async () => {
      await removeUserRole(selectedUserId.value!, roleId)
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

  async function removePermissionFromSelectedRole(permissionId: string): Promise<void> {
    if (!selectedRoleId.value) {
      return
    }

    await saveAction(async () => {
      await removeRolePermission(selectedRoleId.value!, permissionId)
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

  async function loadResource(action: () => Promise<void>): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      await action()
    } catch (caughtError) {
      error.value = mapApiError(caughtError)
    } finally {
      isLoading.value = false
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
    loadUsers,
    loadUsersAndRoles,
    loadRoles,
    loadRolesAndPermissions,
    loadPermissions,
    createUser,
    updateUser,
    deleteUser,
    createRole,
    updateRole,
    deleteRole,
    createPermission,
    updatePermission,
    deletePermission,
    selectUser,
    assignRoleToSelectedUser,
    removeRoleFromSelectedUser,
    selectRole,
    assignPermissionToSelectedRole,
    removePermissionFromSelectedRole,
  }
})
