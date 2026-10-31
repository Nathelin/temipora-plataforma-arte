<script setup>
defineProps({
  titulo: { type: String, required: true },
  detalle: { type: String, default: '' },
  to: { type: [String, Object], required: true },
  icono: { type: String, required: true },
  tono: { type: String, default: 'info' },
  meta: { type: String, default: '' },
})
</script>

<template>
  <router-link
    :to="to"
    class="group flex items-start gap-3 rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10"
  >
    <span
      class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border"
      :class="tono === 'alerta'
        ? 'border-coral/25 bg-coral/10 text-coral'
        : 'border-mantequilla bg-mantequilla/40 text-granate'"
    >
      <svg class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" :d="icono" />
      </svg>
    </span>
    <span class="min-w-0 flex-1">
      <span class="block text-sm font-semibold text-granate">{{ titulo }}</span>
      <span v-if="detalle" class="mt-0.5 block text-xs leading-relaxed text-cafe/60">{{ detalle }}</span>
      <span v-if="meta" class="mt-1 block text-[11px] font-semibold uppercase tracking-[0.12em] text-coral">{{ meta }}</span>
    </span>
    <svg class="mt-2 h-4 w-4 shrink-0 text-cafe/35 transition group-hover:translate-x-0.5 group-hover:text-coral" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
      <path stroke-linecap="round" stroke-linejoin="round" d="m9 5 7 7-7 7" />
    </svg>
  </router-link>
</template>
