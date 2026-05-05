<script setup lang="ts">
import { onMounted } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import AuditLogFiltersForm from '@/features/admin-reports/ui/AuditLogFiltersForm.vue'
import AuditLogsTable from '@/features/admin-reports/ui/AuditLogsTable.vue'
import { useAdminReportsStore } from '@/features/admin-reports/model/adminReports.store'
import type { AuditLogFilters } from '@/features/admin-reports/api/adminReports.api'

const reportsStore = useAdminReportsStore()

onMounted(() => {
  void reportsStore.loadAudit()
})

function applyAuditFilters(filters: AuditLogFilters): void {
  void reportsStore.loadAudit(filters)
}
</script>

<template>
  <main class="admin-audit-logs-page">
    <BaseCard title="Audit log filters" description="Filter administrative audit records by actor, action and entity.">
      <AuditLogFiltersForm :loading="reportsStore.isLoadingAuditLogs" @submit="applyAuditFilters" />
    </BaseCard>

    <AppLoader v-if="reportsStore.isLoadingAuditLogs" label="Loading audit logs..." />
    <AppErrorMessage
      v-else-if="reportsStore.auditError"
      title="Could not load audit logs"
      :message="reportsStore.auditError.message"
    />
    <AuditLogsTable v-else :audit-logs="reportsStore.auditLogs" />
  </main>
</template>

<style scoped>
.admin-audit-logs-page {
  display: grid;
  gap: 20px;
}
</style>
