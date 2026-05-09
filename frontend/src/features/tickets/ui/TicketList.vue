<script setup lang="ts">
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseTable from '@/shared/ui/BaseTable.vue'
import type { Invoice, Ticket } from '@/shared/api/generated/types.gen'

defineProps<{
  tickets: Ticket[]
  invoicesByTicketId: Record<string, Invoice>
  invoiceLoadingTicketId?: string | null
}>()

defineEmits<{
  createInvoice: [ticketId: string]
  refreshInvoice: [ticketId: string]
}>()

const columns = [
  { key: 'drawId', label: 'Draw' },
  { key: 'status', label: 'Status' },
  { key: 'combinationValues', label: 'Combination' },
  { key: 'priceAmount', label: 'Price', align: 'right' },
  { key: 'invoice', label: 'Invoice' },
  { key: 'actions', label: 'Actions' },
] satisfies Array<{ key: keyof Ticket | 'invoice' | 'actions'; label: string; align?: 'left' | 'right' | 'center' }>
</script>

<template>
  <BaseCard title="Tickets" description="Your created tickets and payment invoices.">
    <BaseTable :columns="columns" :rows="tickets" empty-message="No tickets yet">
      <template #combinationValues="{ value }">
        {{ Array.isArray(value) ? value.join(', ') : value }}
      </template>

      <template #priceAmount="{ row }">
        {{ row.priceAmount }} {{ row.priceCurrency }}
      </template>

      <template #invoice="{ row }">
        <div class="ticket-list__invoice">
          <span v-if="invoicesByTicketId[row.id]">{{ invoicesByTicketId[row.id].status }}</span>
          <a
            v-if="invoicesByTicketId[row.id]?.paymentUrl"
            class="ticket-list__payment-link"
            :href="invoicesByTicketId[row.id].paymentUrl"
            target="_blank"
            rel="noreferrer"
          >
            Payment page
          </a>
          <BaseButton
            v-else
            size="sm"
            variant="secondary"
            :loading="invoiceLoadingTicketId === row.id"
            @click="$emit('createInvoice', row.id)"
          >
            Create invoice
          </BaseButton>
        </div>
      </template>

      <template #actions="{ row }">
        <RouterLink class="ticket-list__payment-link" :to="`/account/tickets/${row.id}`">Details</RouterLink>
      </template>
    </BaseTable>
  </BaseCard>
</template>

<style scoped>
.ticket-list__invoice {
  display: flex;
  min-width: 180px;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.ticket-list__payment-link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}

.ticket-list__payment-link:hover {
  text-decoration: underline;
}
</style>
