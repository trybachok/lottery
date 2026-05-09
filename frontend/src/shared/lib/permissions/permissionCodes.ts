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
  DRAW_CANCEL: 'draw.cancel',
  DRAW_RUN: 'draw.run',
  DRAW_RESULT_READ: 'draw.result.read',
  TICKET_READ: 'ticket.read',
  TICKET_CREATE: 'ticket.create',
  TICKET_CANCEL: 'ticket.cancel',
  PAYMENT_READ: 'payment.read',
  PAYMENT_REFUND: 'payment.refund',
  REPORT_DRAW_EXPORT: 'report.draw.export',
  REPORT_TICKET_EXPORT: 'report.ticket.export',
  UI_THEME_MANAGE: 'ui.theme.manage',
  UI_TEMPLATE_MANAGE: 'ui.template.manage',
  AUDIT_READ: 'audit.read',
  SYSTEM_SETTINGS_MANAGE: 'system.settings.manage',
} as const

export const SystemPermissionCodes = Object.values(PermissionCodes)

export const UserAdminPermissions = [
  PermissionCodes.USER_READ,
  PermissionCodes.USER_CREATE,
  PermissionCodes.USER_UPDATE,
  PermissionCodes.USER_DELETE,
] as const
