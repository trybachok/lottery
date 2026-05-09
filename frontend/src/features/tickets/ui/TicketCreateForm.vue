<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'

export type TicketCreateFormValue = {
  drawId: string
  combinationValues: string[]
  priceAmount: string
  priceCurrency: string
}

const props = withDefaults(
  defineProps<{
    loading?: boolean
    errorMessage?: string
    initialDrawId?: string
  }>(),
  {
    loading: false,
    errorMessage: undefined,
    initialDrawId: '',
  },
)

const emit = defineEmits<{
  submit: [value: TicketCreateFormValue]
}>()

const form = reactive({
  drawId: props.initialDrawId,
  combinationValues: '',
  priceAmount: '100.00',
  priceCurrency: 'RUB',
})

const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

watch(
  () => props.initialDrawId,
  (drawId) => {
    if (drawId && form.drawId !== drawId) {
      form.drawId = drawId
    }
  },
  { immediate: true },
)

function submit(): void {
  validationErrors.value = validate()

  if (Object.keys(validationErrors.value).length > 0) {
    return
  }

  emit('submit', {
    drawId: form.drawId.trim(),
    combinationValues: parseCombinationValues(form.combinationValues),
    priceAmount: form.priceAmount.trim(),
    priceCurrency: form.priceCurrency.trim().toUpperCase(),
  })
}

function validate(): Partial<Record<keyof typeof form, string>> {
  const errors: Partial<Record<keyof typeof form, string>> = {}

  if (!form.drawId.trim()) {
    errors.drawId = 'Enter draw id.'
  }

  if (parseCombinationValues(form.combinationValues).length === 0) {
    errors.combinationValues = 'Enter at least one combination value.'
  }

  if (!form.priceAmount.trim() || Number(form.priceAmount) <= 0) {
    errors.priceAmount = 'Enter a positive price.'
  }

  if (!form.priceCurrency.trim()) {
    errors.priceCurrency = 'Enter currency.'
  }

  return errors
}

function parseCombinationValues(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}
</script>

<template>
  <form class="ticket-create-form" novalidate @submit.prevent="submit">
    <AppErrorMessage :message="errorMessage" title="Could not create ticket" />

    <BaseInput
      id="ticket-draw-id"
      v-model="form.drawId"
      label="Draw id"
      placeholder="Draw UUID"
      :error="validationErrors.drawId"
      :disabled="loading"
    />

    <BaseInput
      id="ticket-combination"
      v-model="form.combinationValues"
      label="Combination"
      placeholder="12, 18, 27, 34"
      :error="validationErrors.combinationValues"
      :disabled="loading"
    />

    <div class="ticket-create-form__grid">
      <BaseInput
        id="ticket-price"
        v-model="form.priceAmount"
        label="Price"
        type="number"
        :error="validationErrors.priceAmount"
        :disabled="loading"
      />

      <BaseInput
        id="ticket-currency"
        v-model="form.priceCurrency"
        label="Currency"
        :error="validationErrors.priceCurrency"
        :disabled="loading"
      />
    </div>

    <BaseButton type="submit" :loading="loading">Create ticket</BaseButton>
  </form>
</template>

<style scoped>
.ticket-create-form {
  display: grid;
  gap: 16px;
}

.ticket-create-form__grid {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
}

@media (max-width: 560px) {
  .ticket-create-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
