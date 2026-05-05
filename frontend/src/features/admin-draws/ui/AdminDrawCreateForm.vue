<script setup lang="ts">
import { reactive, ref } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { CreateDrawRequest } from '@/shared/api/generated/types.gen'

withDefaults(
  defineProps<{
    loading?: boolean
    errorMessage?: string
  }>(),
  {
    loading: false,
    errorMessage: undefined,
  },
)

const emit = defineEmits<{
  submit: [value: CreateDrawRequest]
}>()

const form = reactive({
  title: '',
  description: '',
  managerId: '',
  combinationSchemaId: '',
  salesStartAt: '',
  salesEndAt: '',
  drawAt: '',
  maxTickets: '',
  test: false,
})

const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})

function submit(): void {
  validationErrors.value = validate()

  if (Object.keys(validationErrors.value).length > 0) {
    return
  }

  emit('submit', {
    title: form.title.trim(),
    description: form.description.trim() || undefined,
    managerId: form.managerId.trim() || undefined,
    combinationSchemaId: form.combinationSchemaId.trim(),
    salesStartAt: toIsoString(form.salesStartAt),
    salesEndAt: toIsoString(form.salesEndAt),
    drawAt: toIsoString(form.drawAt),
    maxTickets: form.maxTickets ? Number(form.maxTickets) : undefined,
    test: form.test,
  })
}

function validate(): Partial<Record<keyof typeof form, string>> {
  const errors: Partial<Record<keyof typeof form, string>> = {}

  if (!form.title.trim()) {
    errors.title = 'Enter title.'
  }

  if (!form.combinationSchemaId.trim()) {
    errors.combinationSchemaId = 'Enter combination schema id.'
  }

  if (!form.salesStartAt) {
    errors.salesStartAt = 'Select sales start.'
  }

  if (!form.salesEndAt) {
    errors.salesEndAt = 'Select sales end.'
  }

  if (!form.drawAt) {
    errors.drawAt = 'Select draw time.'
  }

  if (form.maxTickets && Number(form.maxTickets) <= 0) {
    errors.maxTickets = 'Max tickets must be positive.'
  }

  return errors
}

function toIsoString(value: string): string {
  return new Date(value).toISOString()
}
</script>

<template>
  <form class="admin-draw-create-form" novalidate @submit.prevent="submit">
    <AppErrorMessage :message="errorMessage" title="Could not create draw" />

    <div class="admin-draw-create-form__grid">
      <BaseInput
        id="admin-draw-title"
        v-model="form.title"
        label="Title"
        :error="validationErrors.title"
        :disabled="loading"
      />
      <BaseInput
        id="admin-draw-schema"
        v-model="form.combinationSchemaId"
        label="Combination schema id"
        :error="validationErrors.combinationSchemaId"
        :disabled="loading"
      />
    </div>

    <BaseInput
      id="admin-draw-description"
      v-model="form.description"
      label="Description"
      :disabled="loading"
    />

    <div class="admin-draw-create-form__grid">
      <BaseInput
        id="admin-draw-sales-start"
        v-model="form.salesStartAt"
        label="Sales start"
        type="datetime-local"
        :error="validationErrors.salesStartAt"
        :disabled="loading"
      />
      <BaseInput
        id="admin-draw-sales-end"
        v-model="form.salesEndAt"
        label="Sales end"
        type="datetime-local"
        :error="validationErrors.salesEndAt"
        :disabled="loading"
      />
    </div>

    <div class="admin-draw-create-form__grid">
      <BaseInput
        id="admin-draw-at"
        v-model="form.drawAt"
        label="Draw time"
        type="datetime-local"
        :error="validationErrors.drawAt"
        :disabled="loading"
      />
      <BaseInput
        id="admin-draw-max"
        v-model="form.maxTickets"
        label="Max tickets"
        type="number"
        :error="validationErrors.maxTickets"
        :disabled="loading"
      />
    </div>

    <div class="admin-draw-create-form__grid">
      <BaseInput
        id="admin-draw-manager"
        v-model="form.managerId"
        label="Manager id"
        :disabled="loading"
      />
      <label class="admin-draw-create-form__checkbox">
        <input v-model="form.test" type="checkbox" :disabled="loading" />
        <span>Test draw</span>
      </label>
    </div>

    <BaseButton type="submit" :loading="loading">Create draw</BaseButton>
  </form>
</template>

<style scoped>
.admin-draw-create-form {
  display: grid;
  gap: 16px;
}

.admin-draw-create-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.admin-draw-create-form__checkbox {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 8px;
  align-self: end;
  color: var(--color-text);
  font-weight: 700;
}

@media (max-width: 720px) {
  .admin-draw-create-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
