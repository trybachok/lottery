export const PermissionCodes = {
  USER_READ: 'user.read',
  USER_CREATE: 'user.create',
  USER_UPDATE: 'user.update',
  USER_DELETE: 'user.delete',
  ROLE_READ: 'role.read',
  ROLE_MANAGE: 'role.manage',
  PERMISSION_MANAGE: 'permission.manage',
  DRAW_READ: 'draw.read',
  DRAW_CREATE: 'draw.create',
  DRAW_UPDATE: 'draw.update',
  DRAW_RUN: 'draw.run',
  REPORT_DRAW_EXPORT: 'report.draw.export',
  REPORT_TICKET_EXPORT: 'report.ticket.export',
  AUDIT_READ: 'audit.read',
} as const

export const UserAdminPermissions = [
  PermissionCodes.USER_READ,
  PermissionCodes.USER_CREATE,
  PermissionCodes.USER_UPDATE,
  PermissionCodes.USER_DELETE,
] as const

