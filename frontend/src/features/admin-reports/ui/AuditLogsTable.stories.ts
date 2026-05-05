import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AuditLogsTable from './AuditLogsTable.vue'
import type { AuditLog } from '@/shared/api/generated/types.gen'

const meta: Meta<typeof AuditLogsTable> = {
  title: 'Features/AdminReports/AuditLogsTable',
  component: AuditLogsTable,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AuditLogsTable>

const auditLogs: AuditLog[] = [
  {
    id: 'audit-1',
    actorUserId: 'admin-1',
    actorRoleCodes: ['ADMIN'],
    action: 'DRAW_RUN',
    entityType: 'draw',
    entityId: 'draw-1',
    requestId: 'req-1',
    ipAddress: '127.0.0.1',
    userAgent: 'Storybook',
    createdAt: '2026-05-05T15:02:00Z',
  },
  {
    id: 'audit-2',
    actorRoleCodes: ['ADMIN'],
    action: 'REPORT_EXPORT',
    entityType: 'ticket_report',
    requestId: 'req-2',
    createdAt: '2026-05-05T15:10:00Z',
  },
]

export const Default: Story = {
  args: {
    auditLogs,
  },
}

export const Empty: Story = {
  args: {
    auditLogs: [],
  },
}
