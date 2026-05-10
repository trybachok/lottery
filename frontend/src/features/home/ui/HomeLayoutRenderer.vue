<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { UiTemplate } from '@/shared/api/generated/types.gen'
import ThemeSwitcher from './ThemeSwitcher.vue'

type LinkAction = {
  label?: string
  to?: string
}

type LayoutBlock = {
  type?: string
  title?: string
  text?: string
  label?: string
  to?: string
}

type LayoutRegion = {
  type?: string
  title?: string
  subtitle?: string
  text?: string
  action?: LinkAction
  actions?: LinkAction[]
  blocks?: LayoutBlock[]
}

const props = defineProps<{
  template: UiTemplate
}>()

const regions = computed<Record<string, LayoutRegion>>(() => {
  const rawRegions = props.template.layout.regions
  return typeof rawRegions === 'object' && rawRegions !== null ? (rawRegions as Record<string, LayoutRegion>) : {}
})

const header = computed(() => regions.value.header ?? {})
const banner = computed(() => regions.value.banner ?? {})
const sidebar = computed(() => regions.value.sidebar ?? {})
const main = computed(() => regions.value.main ?? {})
const footer = computed(() => regions.value.footer ?? {})
</script>

<template>
  <div class="home-layout">
    <header class="home-layout__header">
      <RouterLink class="home-layout__brand" to="/">
        <span class="home-layout__brand-mark" aria-hidden="true">L</span>
        <span>
          <strong>{{ header.title ?? 'Lottery' }}</strong>
          <small v-if="header.subtitle">{{ header.subtitle }}</small>
        </span>
      </RouterLink>
      <nav class="home-layout__nav" aria-label="Main">
        <RouterLink
          v-for="action in header.actions ?? []"
          :key="`${action.label}-${action.to}`"
          :to="action.to ?? '/'"
          class="home-layout__nav-link"
        >
          {{ action.label }}
        </RouterLink>
        <ThemeSwitcher />
      </nav>
    </header>

    <section class="home-layout__banner">
      <div>
        <p class="home-layout__eyebrow">{{ template.name }}</p>
        <h1>{{ banner.title }}</h1>
        <p>{{ banner.subtitle }}</p>
      </div>
      <RouterLink v-if="banner.action?.to" class="home-layout__button" :to="banner.action.to">
        {{ banner.action.label }}
      </RouterLink>
    </section>

    <div class="home-layout__content">
      <aside class="home-layout__sidebar">
        <h2>{{ sidebar.title }}</h2>
        <div class="home-layout__side-blocks">
          <template v-for="block in sidebar.blocks ?? []" :key="`${block.type}-${block.label ?? block.text}`">
            <RouterLink v-if="block.type === 'link'" class="home-layout__side-link" :to="block.to ?? '/'">
              {{ block.label }}
            </RouterLink>
            <p v-else class="home-layout__side-text">{{ block.text }}</p>
          </template>
        </div>
      </aside>

      <main class="home-layout__main">
        <h2>{{ main.title }}</h2>
        <div class="home-layout__cards">
          <article v-for="block in main.blocks ?? []" :key="`${block.title}-${block.text}`" class="home-layout__card">
            <h3>{{ block.title }}</h3>
            <p>{{ block.text }}</p>
          </article>
        </div>
      </main>
    </div>

    <footer class="home-layout__footer">
      {{ footer.text }}
    </footer>
  </div>
</template>

<style scoped>
.home-layout {
  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text);
}

.home-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  padding: 18px clamp(18px, 5vw, 64px);
}

.home-layout__brand,
.home-layout__nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.home-layout__brand {
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.home-layout__brand strong,
.home-layout__brand small {
  display: block;
}

.home-layout__brand small {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  line-height: 1.35;
}

.home-layout__brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-text, #fff);
  font-weight: 800;
}

.home-layout__nav {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.home-layout__nav-link,
.home-layout__button,
.home-layout__side-link {
  color: inherit;
  font-weight: 650;
  text-decoration: none;
}

.home-layout__nav-link {
  color: var(--color-text-muted);
}

.home-layout__banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  background: var(--color-home-banner, var(--color-primary-soft));
  padding: clamp(32px, 7vw, 82px) clamp(18px, 5vw, 64px);
}

.home-layout__eyebrow {
  margin: 0 0 10px;
  color: var(--color-primary);
  font-weight: 750;
}

.home-layout__banner h1,
.home-layout__main h2,
.home-layout__sidebar h2,
.home-layout__card h3 {
  margin: 0;
}

.home-layout__banner h1 {
  max-width: 780px;
  font-size: clamp(2rem, 6vw, 4.25rem);
  line-height: 1.02;
}

.home-layout__banner p {
  max-width: 660px;
  margin: 14px 0 0;
  color: var(--color-text-muted);
  font-size: 1.05rem;
  line-height: 1.6;
}

.home-layout__button {
  flex: 0 0 auto;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-text, #fff);
  padding: 14px 18px;
}

.home-layout__content {
  display: grid;
  grid-template-columns: minmax(220px, 300px) minmax(0, 1fr);
  gap: 22px;
  padding: 24px clamp(18px, 5vw, 64px);
}

.home-layout__sidebar,
.home-layout__card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 18px;
}

.home-layout__sidebar {
  align-self: start;
  background: var(--color-home-sidebar, var(--color-surface));
}

.home-layout__side-blocks,
.home-layout__main {
  display: grid;
  gap: 14px;
}

.home-layout__side-link {
  display: block;
  border-bottom: 1px solid var(--color-border);
  padding: 10px 0;
}

.home-layout__side-text,
.home-layout__card p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.home-layout__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.home-layout__card {
  display: grid;
  gap: 10px;
}

.home-layout__footer {
  border-top: 1px solid var(--color-border);
  color: var(--color-text-muted);
  padding: 20px clamp(18px, 5vw, 64px);
}

@media (max-width: 760px) {
  .home-layout__header,
  .home-layout__banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .home-layout__nav {
    justify-content: flex-start;
  }

  .home-layout__content {
    grid-template-columns: 1fr;
  }
}
</style>
