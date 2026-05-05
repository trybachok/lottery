<script setup lang="ts" generic="TRow extends Record<string, unknown>">
export type BaseTableColumn<TRow> = {
  key: keyof TRow & string
  label: string
  align?: 'left' | 'right' | 'center'
}

defineProps<{
  columns: Array<BaseTableColumn<TRow>>
  rows: TRow[]
  emptyMessage?: string
}>()
</script>

<template>
  <div class="base-table">
    <table>
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.key" :class="`base-table__cell--${column.align ?? 'left'}`">
            {{ column.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, rowIndex) in rows" :key="String(row.id ?? rowIndex)">
          <td v-for="column in columns" :key="column.key" :class="`base-table__cell--${column.align ?? 'left'}`">
            <slot :name="column.key" :row="row" :value="row[column.key]">
              {{ row[column.key] }}
            </slot>
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td class="base-table__empty" :colspan="columns.length">
            {{ emptyMessage ?? 'No data' }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.base-table {
  width: 100%;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

th,
td {
  border-bottom: 1px solid var(--color-border);
  padding: 11px 14px;
  white-space: nowrap;
}

th {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-weight: 700;
}

tbody tr:last-child td {
  border-bottom: 0;
}

.base-table__cell--left {
  text-align: left;
}

.base-table__cell--center {
  text-align: center;
}

.base-table__cell--right {
  text-align: right;
}

.base-table__empty {
  color: var(--color-text-muted);
  text-align: center;
}
</style>
