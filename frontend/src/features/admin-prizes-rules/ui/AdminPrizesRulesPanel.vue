<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import AppErrorMessage from '@/shared/ui/AppErrorMessage.vue'
import AppLoader from '@/shared/ui/AppLoader.vue'
import BaseButton from '@/shared/ui/BaseButton.vue'
import BaseCard from '@/shared/ui/BaseCard.vue'
import BaseInput from '@/shared/ui/BaseInput.vue'
import BaseSelect from '@/shared/ui/BaseSelect.vue'
import type {
  Draw,
  Prize,
  PrizeRequest,
  PrizeType,
  WinningRuleRequest,
} from '@/shared/api/generated/types.gen'

const props = defineProps<{
  draws: Draw[]
  prizes: Prize[]
  winningRules: WinningRuleRequest[]
  selectedDrawId?: string
  editingPrizeId?: string | null
  loadingPrizes?: boolean
  loadingRules?: boolean
  savingPrize?: boolean
  savingRules?: boolean
  errorMessage?: string
  actionErrorMessage?: string
  feedbackMessage?: string | null
}>()

const emit = defineEmits<{
  savePrize: [request: PrizeRequest]
  editPrize: [prize: Prize]
  cancelPrizeEdit: []
  loadRules: [drawId: string]
  saveRules: [drawId: string, rules: WinningRuleRequest[]]
}>()

const editableStatuses = new Set(['DRAFT', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'POSTPONED'])
const prizeForm = reactive({
  type: 'MONEY' as PrizeType,
  name: 'Demo main prize',
  amount: '1000',
  currency: 'RUB',
  productId: '',
  quantity: '',
  unit: '',
})
const ruleForm = reactive({
  drawId: '',
  matchPercentFrom: '100',
  matchPercentTo: '100',
  prizeId: '',
  priority: '1',
})
const draftRules = ref<WinningRuleRequest[]>([])
const validationErrors = ref<Record<string, string>>({})

const drawOptions = computed(() => [
  { label: 'Select draw', value: '' },
  ...props.draws.map((draw) => ({
    label: `${draw.title} (${draw.status})`,
    value: draw.id,
  })),
])

const prizeOptions = computed(() => [
  { label: 'Select prize', value: '' },
  ...props.prizes.map((prize) => ({
    label: `${prize.name} (${formatPrize(prize)})`,
    value: prize.id,
  })),
])

const selectedDraw = computed(() => props.draws.find((draw) => draw.id === ruleForm.drawId))
const selectedDrawEditable = computed(() => Boolean(selectedDraw.value && editableStatuses.has(selectedDraw.value.status)))
const selectedPrize = computed(() => props.prizes.find((prize) => prize.id === props.editingPrizeId) ?? null)

watch(
  () => props.selectedDrawId,
  (drawId) => {
    if (drawId && ruleForm.drawId !== drawId) {
      ruleForm.drawId = drawId
    }
  },
  { immediate: true },
)

watch(
  () => props.winningRules,
  (rules) => {
    draftRules.value = rules.map((rule) => ({
      matchPercentFrom: rule.matchPercentFrom,
      matchPercentTo: rule.matchPercentTo,
      prizeId: rule.prizeId,
      priority: rule.priority,
    }))
  },
  { immediate: true },
)

watch(selectedPrize, (prize) => {
  if (!prize) return
  prizeForm.type = prize.type
  prizeForm.name = prize.name
  prizeForm.amount = String(prize.amount ?? '')
  prizeForm.currency = prize.currency ?? 'RUB'
  prizeForm.productId = prize.productId ?? ''
  prizeForm.quantity = String(prize.quantity ?? '')
  prizeForm.unit = prize.unit ?? ''
})

function savePrize(): void {
  validationErrors.value = validatePrize()
  if (Object.keys(validationErrors.value).length > 0) return
  emit('savePrize', buildPrizeRequest())
}

function cancelPrizeEdit(): void {
  resetPrizeForm()
  emit('cancelPrizeEdit')
}

function selectDraw(drawId: string): void {
  ruleForm.drawId = drawId
  draftRules.value = []
  if (drawId) {
    emit('loadRules', drawId)
  }
}

function addRule(): void {
  validationErrors.value = validateRule()
  if (Object.keys(validationErrors.value).length > 0) return
  draftRules.value = [
    ...draftRules.value,
    {
      matchPercentFrom: Number(ruleForm.matchPercentFrom),
      matchPercentTo: Number(ruleForm.matchPercentTo),
      prizeId: ruleForm.prizeId,
      priority: Number(ruleForm.priority),
    },
  ].sort((left, right) => left.priority - right.priority)
}

function removeRule(index: number): void {
  draftRules.value = draftRules.value.filter((_, ruleIndex) => ruleIndex !== index)
}

function saveRules(): void {
  validationErrors.value = {}
  if (!ruleForm.drawId) {
    validationErrors.value.drawId = 'Select draw.'
    return
  }
  if (draftRules.value.length === 0) {
    validationErrors.value.rules = 'Add at least one rule.'
    return
  }
  emit('saveRules', ruleForm.drawId, draftRules.value)
}

function validatePrize(): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!prizeForm.name.trim()) errors.prizeName = 'Enter prize name.'
  if (prizeForm.type === 'MONEY') {
    if (!isPositiveOrZero(prizeForm.amount)) errors.amount = 'Enter non-negative amount.'
    if (!/^[A-Z]{3}$/.test(prizeForm.currency.trim().toUpperCase())) errors.currency = 'Enter 3-letter currency.'
    return errors
  }
  if (!uuidLike(prizeForm.productId)) errors.productId = 'Enter product UUID.'
  if (!isPositiveOrZero(prizeForm.quantity)) errors.quantity = 'Enter non-negative quantity.'
  if (!prizeForm.unit.trim()) errors.unit = 'Enter unit.'
  return errors
}

function validateRule(): Record<string, string> {
  const errors: Record<string, string> = {}
  const from = Number(ruleForm.matchPercentFrom)
  const to = Number(ruleForm.matchPercentTo)
  if (!ruleForm.drawId) errors.drawId = 'Select draw.'
  if (!selectedDrawEditable.value) errors.drawId = 'Rules can be changed only before sales are closed.'
  if (!ruleForm.prizeId) errors.prizeId = 'Select prize.'
  if (!isPercentStep(from)) errors.matchPercentFrom = 'Use 0..100 with 5 percent step.'
  if (!isPercentStep(to)) errors.matchPercentTo = 'Use 0..100 with 5 percent step.'
  if (from > to) errors.matchPercentTo = 'To must not be less than from.'
  if (!Number.isInteger(Number(ruleForm.priority)) || Number(ruleForm.priority) < 0) {
    errors.priority = 'Enter non-negative priority.'
  }
  return errors
}

function buildPrizeRequest(): PrizeRequest {
  if (prizeForm.type === 'MONEY') {
    return {
      type: prizeForm.type,
      name: prizeForm.name.trim(),
      amount: Number(prizeForm.amount),
      currency: prizeForm.currency.trim().toUpperCase(),
    }
  }
  return {
    type: prizeForm.type,
    name: prizeForm.name.trim(),
    productId: prizeForm.productId.trim(),
    quantity: Number(prizeForm.quantity),
    unit: prizeForm.unit.trim(),
  }
}

function resetPrizeForm(): void {
  prizeForm.type = 'MONEY'
  prizeForm.name = 'Demo main prize'
  prizeForm.amount = '1000'
  prizeForm.currency = 'RUB'
  prizeForm.productId = ''
  prizeForm.quantity = ''
  prizeForm.unit = ''
}

function formatPrize(prize: Prize): string {
  if (prize.type === 'MONEY') {
    return `${prize.amount ?? '-'} ${prize.currency ?? ''}`.trim()
  }
  return `${prize.quantity ?? '-'} ${prize.unit ?? ''}`.trim()
}

function prizeName(prizeId: string): string {
  return props.prizes.find((prize) => prize.id === prizeId)?.name ?? prizeId
}

function isPositiveOrZero(value: string): boolean {
  return value.trim() !== '' && Number(value) >= 0
}

function isPercentStep(value: number): boolean {
  return Number.isFinite(value) && value >= 0 && value <= 100 && value % 5 === 0
}

function uuidLike(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.trim())
}
</script>

<template>
  <section class="admin-prizes-rules-panel">
    <AppLoader v-if="loadingPrizes" label="Loading prizes..." />
    <AppErrorMessage v-if="errorMessage" title="Could not load prizes" :message="errorMessage" />
    <AppErrorMessage v-if="actionErrorMessage" title="Action failed" :message="actionErrorMessage" />
    <BaseCard v-if="feedbackMessage" title="Saved">
      <p class="admin-prizes-rules-panel__muted">{{ feedbackMessage }}</p>
    </BaseCard>

    <BaseCard title="Prizes" description="Create reusable prizes for winning rules.">
      <form class="admin-prizes-rules-panel__form" novalidate @submit.prevent="savePrize">
        <BaseSelect
          id="prize-type"
          v-model="prizeForm.type"
          label="Type"
          :options="[
            { label: 'Money', value: 'MONEY' },
            { label: 'Food product', value: 'FOOD_PRODUCT' },
            { label: 'Sport supplement', value: 'SPORT_SUPPLEMENT' },
          ]"
          :disabled="savingPrize"
        />
        <BaseInput
          id="prize-name"
          v-model="prizeForm.name"
          label="Name"
          :error="validationErrors.prizeName"
          :disabled="savingPrize"
        />
        <template v-if="prizeForm.type === 'MONEY'">
          <BaseInput
            id="prize-amount"
            v-model="prizeForm.amount"
            label="Amount"
            type="number"
            :error="validationErrors.amount"
            :disabled="savingPrize"
          />
          <BaseInput
            id="prize-currency"
            v-model="prizeForm.currency"
            label="Currency"
            :error="validationErrors.currency"
            :disabled="savingPrize"
          />
        </template>
        <template v-else>
          <BaseInput
            id="prize-product-id"
            v-model="prizeForm.productId"
            label="Product ID"
            :error="validationErrors.productId"
            :disabled="savingPrize"
          />
          <BaseInput
            id="prize-quantity"
            v-model="prizeForm.quantity"
            label="Quantity"
            type="number"
            :error="validationErrors.quantity"
            :disabled="savingPrize"
          />
          <BaseInput
            id="prize-unit"
            v-model="prizeForm.unit"
            label="Unit"
            :error="validationErrors.unit"
            :disabled="savingPrize"
          />
        </template>
        <div class="admin-prizes-rules-panel__actions">
          <BaseButton type="submit" :loading="savingPrize">
            {{ editingPrizeId ? 'Save prize' : 'Create prize' }}
          </BaseButton>
          <BaseButton v-if="editingPrizeId" variant="secondary" :disabled="savingPrize" @click="cancelPrizeEdit">
            Cancel
          </BaseButton>
        </div>
      </form>

      <div class="admin-prizes-rules-panel__list">
        <article v-for="prize in prizes" :key="prize.id" class="admin-prizes-rules-panel__item">
          <div>
            <strong>{{ prize.name }}</strong>
            <span>{{ prize.type }} · {{ formatPrize(prize) }}</span>
          </div>
          <BaseButton size="sm" variant="secondary" @click="$emit('editPrize', prize)">Edit</BaseButton>
        </article>
        <p v-if="prizes.length === 0 && !loadingPrizes" class="admin-prizes-rules-panel__muted">
          No prizes yet.
        </p>
      </div>
    </BaseCard>

    <BaseCard title="Winning rules" description="Bind prize ranges to an editable draw before sales are closed.">
      <div class="admin-prizes-rules-panel__rules">
        <BaseSelect
          id="rules-draw"
          :model-value="ruleForm.drawId"
          label="Draw"
          :options="drawOptions"
          :error="validationErrors.drawId"
          :disabled="loadingRules || savingRules"
          @update:model-value="selectDraw"
        />
        <AppLoader v-if="loadingRules" label="Loading winning rules..." />

        <div class="admin-prizes-rules-panel__form">
          <BaseInput
            id="rule-from"
            v-model="ruleForm.matchPercentFrom"
            label="Match from"
            type="number"
            :error="validationErrors.matchPercentFrom"
            :disabled="!selectedDrawEditable || savingRules"
          />
          <BaseInput
            id="rule-to"
            v-model="ruleForm.matchPercentTo"
            label="Match to"
            type="number"
            :error="validationErrors.matchPercentTo"
            :disabled="!selectedDrawEditable || savingRules"
          />
          <BaseSelect
            id="rule-prize"
            v-model="ruleForm.prizeId"
            label="Prize"
            :options="prizeOptions"
            :error="validationErrors.prizeId"
            :disabled="!selectedDrawEditable || savingRules || prizes.length === 0"
          />
          <BaseInput
            id="rule-priority"
            v-model="ruleForm.priority"
            label="Priority"
            type="number"
            :error="validationErrors.priority"
            :disabled="!selectedDrawEditable || savingRules"
          />
          <div class="admin-prizes-rules-panel__actions">
            <BaseButton
              variant="secondary"
              :disabled="!selectedDrawEditable || prizes.length === 0 || savingRules"
              @click="addRule"
            >
              Add rule
            </BaseButton>
            <BaseButton
              :loading="savingRules"
              :disabled="!selectedDrawEditable || draftRules.length === 0"
              @click="saveRules"
            >
              Save rules
            </BaseButton>
          </div>
        </div>

        <p v-if="validationErrors.rules" class="admin-prizes-rules-panel__error">{{ validationErrors.rules }}</p>
        <p v-if="selectedDraw && !selectedDrawEditable" class="admin-prizes-rules-panel__muted">
          Rules are locked after sales are closed or result generation starts.
        </p>

        <div class="admin-prizes-rules-panel__table" role="table" aria-label="Winning rules">
          <div class="admin-prizes-rules-panel__row admin-prizes-rules-panel__row--head" role="row">
            <span>Range</span>
            <span>Prize</span>
            <span>Priority</span>
            <span>Actions</span>
          </div>
          <div
            v-for="(rule, index) in draftRules"
            :key="`${rule.prizeId}-${rule.matchPercentFrom}-${rule.matchPercentTo}-${rule.priority}-${index}`"
            class="admin-prizes-rules-panel__row"
            role="row"
          >
            <span>{{ rule.matchPercentFrom }}% - {{ rule.matchPercentTo }}%</span>
            <span>{{ prizeName(rule.prizeId) }}</span>
            <span>{{ rule.priority }}</span>
            <BaseButton
              size="sm"
              variant="secondary"
              :disabled="!selectedDrawEditable || savingRules"
              @click="removeRule(index)"
            >
              Remove
            </BaseButton>
          </div>
          <p v-if="draftRules.length === 0" class="admin-prizes-rules-panel__muted">No winning rules for selected draw.</p>
        </div>
      </div>
    </BaseCard>
  </section>
</template>

<style scoped>
.admin-prizes-rules-panel {
  display: grid;
  gap: 20px;
}

.admin-prizes-rules-panel__form {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 12px;
  align-items: end;
}

.admin-prizes-rules-panel__actions,
.admin-prizes-rules-panel__list,
.admin-prizes-rules-panel__rules {
  display: grid;
  gap: 12px;
}

.admin-prizes-rules-panel__actions {
  grid-template-columns: repeat(2, max-content);
}

.admin-prizes-rules-panel__item,
.admin-prizes-rules-panel__row {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 2fr) 100px 120px;
  gap: 12px;
  align-items: center;
  border-top: 1px solid var(--color-border);
  padding: 10px 0;
}

.admin-prizes-rules-panel__item {
  grid-template-columns: minmax(0, 1fr) auto;
}

.admin-prizes-rules-panel__item div {
  display: grid;
  gap: 4px;
}

.admin-prizes-rules-panel__item span,
.admin-prizes-rules-panel__muted {
  color: var(--color-text-muted);
}

.admin-prizes-rules-panel__muted,
.admin-prizes-rules-panel__error {
  margin: 0;
}

.admin-prizes-rules-panel__error {
  color: var(--color-danger);
  font-size: 0.875rem;
  font-weight: 650;
}

.admin-prizes-rules-panel__row--head {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 750;
  text-transform: uppercase;
}

@media (max-width: 980px) {
  .admin-prizes-rules-panel__form,
  .admin-prizes-rules-panel__row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-prizes-rules-panel__form,
  .admin-prizes-rules-panel__row,
  .admin-prizes-rules-panel__item {
    grid-template-columns: 1fr;
  }
}
</style>
