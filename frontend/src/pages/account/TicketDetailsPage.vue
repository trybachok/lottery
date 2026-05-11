<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import InvoiceStatusPanel from '@/features/tickets/ui/InvoiceStatusPanel.vue'
import TicketDetailsCard from '@/features/tickets/ui/TicketDetailsCard.vue'
import { useTicketsStore } from '@/features/tickets/model/tickets.store'

const route = useRoute()
const ticketsStore = useTicketsStore()
const ticketId = computed(() => String(route.params.ticketId ?? ''))

watch(
  ticketId,
  (value) => {
    if (value) {
      void ticketsStore.loadTicketDetails(value)
    }
  },
  { immediate: true },
)

async function createInvoice(): Promise<void> {
  if (ticketsStore.selectedTicket) {
    await ticketsStore.createInvoice(ticketsStore.selectedTicket.id)
  }
}

async function refreshInvoice(): Promise<void> {
  if (ticketsStore.selectedInvoice?.id) {
    await ticketsStore.refreshInvoice(ticketsStore.selectedInvoice.id)
    return
  }

  if (ticketId.value) {
    await ticketsStore.refreshTicketInvoice(ticketId.value, true)
  }
}

async function simulateMockWebhook(eventType: 'PAYMENT_SUCCEEDED' | 'PAYMENT_FAILED'): Promise<void> {
  if (ticketsStore.selectedInvoice?.id) {
    await ticketsStore.simulateMockWebhook(ticketsStore.selectedInvoice.id, eventType)
  }
}

async function checkResult(): Promise<void> {
  if (ticketsStore.selectedTicket) {
    await ticketsStore.checkResult(ticketsStore.selectedTicket.id)
  }
}
</script>

<template>
  <main class="ticket-details-page">
    <AppLoader v-if="ticketsStore.isLoadingDetails" label="Loading ticket..." />
    <AppErrorMessage
      v-else-if="ticketsStore.detailsError"
      title="Could not load ticket"
      :message="ticketsStore.detailsError.message"
    />

    <template v-else-if="ticketsStore.selectedTicket">
      <TicketDetailsCard
        :ticket="ticketsStore.selectedTicket"
        :checking="ticketsStore.isCheckingResult"
        @check-result="checkResult"
      />

      <InvoiceStatusPanel
        :invoice="ticketsStore.selectedInvoice"
        :loading="ticketsStore.invoiceLoadingTicketId === ticketsStore.selectedTicket.id"
        :mock-webhook-loading="ticketsStore.mockWebhookLoadingInvoiceId === ticketsStore.selectedInvoice?.id"
        :can-create="ticketsStore.selectedTicket.status === 'CREATED'"
        @create-invoice="createInvoice"
        @refresh-invoice="refreshInvoice"
        @simulate-mock-webhook="simulateMockWebhook"
      />

      <AppErrorMessage
        v-if="ticketsStore.actionError"
        title="Action failed"
        :message="ticketsStore.actionError.message"
      />

      <div class="ticket-details-page__actions">
        <RouterLink to="/account" custom v-slot="{ navigate }">
          <BaseButton variant="secondary" @click="navigate">Back to account</BaseButton>
        </RouterLink>
      </div>
    </template>
  </main>
</template>

<style scoped>
.ticket-details-page {
  display: grid;
  gap: 20px;
  width: min(100%, 960px);
  margin: 0 auto;
  padding: 32px 20px;
}

.ticket-details-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
