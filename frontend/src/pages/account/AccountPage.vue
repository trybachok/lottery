<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import { useAuthStore } from '@/features/auth/model/auth.store'
import { useTicketsStore } from '@/features/tickets/model/tickets.store'
import ResultHistory from '@/features/tickets/ui/ResultHistory.vue'
import TicketCreateForm, { type TicketCreateFormValue } from '@/features/tickets/ui/TicketCreateForm.vue'
import TicketList from '@/features/tickets/ui/TicketList.vue'

const authStore = useAuthStore()
const ticketsStore = useTicketsStore()
const route = useRoute()
const router = useRouter()
const initialDrawId = computed(() => (typeof route.query.drawId === 'string' ? route.query.drawId : ''))

onMounted(() => {
  void ticketsStore.loadTickets(authStore.user?.id)
})

async function logout(): Promise<void> {
  authStore.logout()
  await router.push('/login')
}

async function createTicket(value: TicketCreateFormValue): Promise<void> {
  if (!authStore.user) {
    return
  }

  const ticket = await ticketsStore.createTicket({
    userId: authStore.user.id,
    drawId: value.drawId,
    combinationValues: value.combinationValues,
    priceAmount: value.priceAmount,
    priceCurrency: value.priceCurrency,
  })

  if (ticket) {
    await router.push(`/account/tickets/${ticket.id}`)
  }
}
</script>

<template>
  <main class="account-page">
    <BaseCard title="Account" description="Your lottery profile and tickets will appear here.">
      <dl class="account-page__details">
        <div>
          <dt>Email</dt>
          <dd>{{ authStore.user?.email }}</dd>
        </div>
        <div>
          <dt>Login</dt>
          <dd>{{ authStore.user?.login }}</dd>
        </div>
      </dl>

      <template #actions>
        <BaseButton variant="secondary" size="sm" @click="logout">Logout</BaseButton>
      </template>
    </BaseCard>

    <BaseCard title="Create ticket" description="Create a ticket for an active draw.">
      <TicketCreateForm
        :initial-draw-id="initialDrawId"
        :loading="ticketsStore.isCreating"
        :error-message="ticketsStore.actionError?.message"
        @submit="createTicket"
      />
    </BaseCard>

    <AppLoader v-if="ticketsStore.isLoading" label="Loading tickets..." />
    <AppErrorMessage
      v-else-if="ticketsStore.error"
      title="Could not load tickets"
      :message="ticketsStore.error.message"
    />
    <TicketList
      v-else
      :tickets="ticketsStore.items"
      :invoices-by-ticket-id="ticketsStore.invoicesByTicketId"
      :invoice-loading-ticket-id="ticketsStore.invoiceLoadingTicketId"
      @create-invoice="ticketsStore.createInvoice"
    />

    <ResultHistory v-if="!ticketsStore.isLoading && !ticketsStore.error" :tickets="ticketsStore.items" />
  </main>
</template>

<style scoped>
.account-page {
  display: grid;
  gap: 20px;
  width: min(100%, 960px);
  margin: 0 auto;
  padding: 32px 20px;
}

.account-page__details {
  display: grid;
  gap: 12px;
  margin: 0;
}

.account-page__details div {
  display: grid;
  gap: 4px;
}

dt {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 700;
}

dd {
  margin: 0;
}
</style>
