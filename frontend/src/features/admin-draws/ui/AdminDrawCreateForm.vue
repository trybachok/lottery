<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import type { CreateDrawRequest } from '@/shared/api/generated/types.gen'

const props = withDefaults(
  defineProps<{
    loading?: boolean
    errorMessage?: string
    canAssignManager?: boolean
    defaultManagerId?: string
  }>(),
  {
    loading: false,
    errorMessage: undefined,
    canAssignManager: true,
    defaultManagerId: undefined,
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
  combinationSchemaName: 'Demo schema: 7 + 1..2',
  combinationSchemaDefinition: `{
  "positions": [
    {
      "type": "NUMBER",
      "min": 7,
      "max": 7
    },
    {
      "type": "NUMBER",
      "min": 1,
      "max": 2
    }
  ],
  "allowDuplicates": false,
  "orderSensitive": true
}`,
  salesStartAt: '',
  salesEndAt: '',
  drawAt: '',
  maxTickets: '',
  test: false,
})

const schemaMode = ref<'new' | 'existing'>('new')
const validationErrors = ref<Partial<Record<keyof typeof form, string>>>({})
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

watch(
  () => props.defaultManagerId,
  (managerId) => {
    if (!props.canAssignManager && managerId) {
      form.managerId = managerId
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
    title: form.title.trim(),
    description: form.description.trim() || undefined,
    managerId: props.canAssignManager ? form.managerId.trim() || undefined : props.defaultManagerId,
    combinationSchemaId: schemaMode.value === 'existing' ? form.combinationSchemaId.trim() : undefined,
    combinationSchema:
      schemaMode.value === 'new'
        ? {
            name: form.combinationSchemaName.trim(),
            definitionJson: form.combinationSchemaDefinition.trim(),
          }
        : undefined,
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

  if (schemaMode.value === 'existing') {
    if (!form.combinationSchemaId.trim()) {
      errors.combinationSchemaId = 'Enter combination schema id.'
    } else if (!uuidPattern.test(form.combinationSchemaId.trim())) {
      errors.combinationSchemaId = 'Enter a valid UUID.'
    }
  } else {
    if (!form.combinationSchemaName.trim()) {
      errors.combinationSchemaName = 'Enter schema name.'
    }
    if (!form.combinationSchemaDefinition.trim()) {
      errors.combinationSchemaDefinition = 'Enter schema JSON.'
    } else {
      try {
        const parsed = JSON.parse(form.combinationSchemaDefinition) as unknown
        if (!isSchemaObject(parsed)) {
          errors.combinationSchemaDefinition = 'Schema JSON must be an object with positions array.'
        }
      } catch {
        errors.combinationSchemaDefinition = 'Enter valid JSON.'
      }
    }
  }

  if (props.canAssignManager && form.managerId.trim() && !uuidPattern.test(form.managerId.trim())) {
    errors.managerId = 'Enter a valid UUID.'
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

function isSchemaObject(value: unknown): value is { positions: unknown[] } {
  return (
    typeof value === 'object' &&
    value !== null &&
    Array.isArray((value as { positions?: unknown }).positions) &&
    (value as { positions: unknown[] }).positions.length > 0
  )
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
      <fieldset class="admin-draw-create-form__schema-mode" :disabled="loading">
        <legend>Combination schema</legend>
        <label>
          <input v-model="schemaMode" type="radio" value="new" />
          <span>Create new</span>
        </label>
        <label>
          <input v-model="schemaMode" type="radio" value="existing" />
          <span>Use existing id</span>
        </label>
      </fieldset>
    </div>

    <template v-if="schemaMode === 'new'">
      <BaseInput
        id="admin-draw-schema-name"
        v-model="form.combinationSchemaName"
        label="Schema name"
        :error="validationErrors.combinationSchemaName"
        :disabled="loading"
      />
      <label class="admin-draw-create-form__textarea">
        <span>Schema JSON</span>
        <textarea
          id="admin-draw-schema-json"
          v-model="form.combinationSchemaDefinition"
          :disabled="loading"
          :aria-invalid="Boolean(validationErrors.combinationSchemaDefinition)"
          :aria-describedby="validationErrors.combinationSchemaDefinition ? 'admin-draw-schema-json-error' : undefined"
          rows="12"
        />
        <small v-if="validationErrors.combinationSchemaDefinition" id="admin-draw-schema-json-error">
          {{ validationErrors.combinationSchemaDefinition }}
        </small>
      </label>
    </template>

    <BaseInput
      v-else
      id="admin-draw-schema"
      v-model="form.combinationSchemaId"
      label="Combination schema id"
      :error="validationErrors.combinationSchemaId"
      :disabled="loading"
    />

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
        v-if="canAssignManager"
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

.admin-draw-create-form__schema-mode {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 12px;
  align-self: end;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  margin: 0;
  padding: 8px 12px;
}

.admin-draw-create-form__schema-mode legend {
  padding: 0 4px;
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 650;
}

.admin-draw-create-form__schema-mode label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text);
  font-weight: 650;
}

.admin-draw-create-form__textarea {
  display: grid;
  gap: 6px;
  color: var(--color-text);
}

.admin-draw-create-form__textarea span {
  font-size: 0.875rem;
  font-weight: 650;
}

.admin-draw-create-form__textarea textarea {
  width: 100%;
  min-height: 220px;
  resize: vertical;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  padding: 12px;
  font: 0.875rem/1.45 ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  outline: none;
}

.admin-draw-create-form__textarea textarea:focus {
  border-color: var(--color-focus);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-focus) 18%, transparent);
}

.admin-draw-create-form__textarea textarea[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.admin-draw-create-form__textarea small {
  color: var(--color-danger);
  font-size: 0.8125rem;
}

@media (max-width: 720px) {
  .admin-draw-create-form__grid {
    grid-template-columns: 1fr;
  }

  .admin-draw-create-form__schema-mode {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
