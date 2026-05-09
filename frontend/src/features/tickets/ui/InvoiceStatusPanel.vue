<script setup lang="ts">
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import type { Invoice } from '@/shared/api/generated/types.gen'

defineProps<{
  invoice: Invoice | null
  loading?: boolean
  canCreate?: boolean
}>()

defineEmits<{
  createInvoice: []
  refreshInvoice: []
}>()

function formatDate(value?: string): string {
  if (!value) return '-'
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <BaseCard title="Payment status" description="Latest invoice and provider payment link.">
    <template #actions>
      <BaseButton v-if="invoice" size="sm" variant="secondary" :loading="loading" @click="$emit('refreshInvoice')">
        Refresh
      </BaseButton>
      <BaseButton v-else-if="canCreate" size="sm" :loading="loading" @click="$emit('createInvoice')">
        Create invoice
      </BaseButton>
    </template>

    <p v-if="!invoice" class="invoice-status-panel__muted">No invoice has been created for this ticket yet.</p>
    <dl v-else class="invoice-status-panel__grid">
      <div>
        <dt>Status</dt>
        <dd>{{ invoice.status }}</dd>
      </div>
      <div>
        <dt>Amount</dt>
        <dd>{{ invoice.amount }} {{ invoice.currency }}</dd>
      </div>
      <div>
        <dt>Provider</dt>
        <dd>{{ invoice.providerCode }}</dd>
      </div>
      <div>
        <dt>Created</dt>
        <dd>{{ formatDate(invoice.createdAt) }}</dd>
      </div>
      <div>
        <dt>Expires</dt>
        <dd>{{ formatDate(invoice.expiresAt) }}</dd>
      </div>
      <div>
        <dt>Paid</dt>
        <dd>{{ formatDate(invoice.paidAt) }}</dd>
      </div>
    </dl>

    <a
      v-if="invoice?.paymentUrl"
      class="invoice-status-panel__payment-link"
      :href="invoice.paymentUrl"
      target="_blank"
      rel="noreferrer"
    >
      Payment page
    </a>
  </BaseCard>
</template>

<style scoped>
.invoice-status-panel__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}

.invoice-status-panel__muted,
dt {
  color: var(--color-text-muted);
}

.invoice-status-panel__muted {
  margin: 0;
}

dt {
  font-size: 0.8125rem;
  font-weight: 700;
}

dd {
  margin: 4px 0 0;
  overflow-wrap: anywhere;
}

.invoice-status-panel__payment-link {
  display: inline-flex;
  margin-top: 16px;
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}

.invoice-status-panel__payment-link:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .invoice-status-panel__grid {
    grid-template-columns: 1fr;
  }
}
</style>
