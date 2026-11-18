<script setup>
import SelectorTema from './SelectorTema.vue'

defineProps({
  abierto: { type: Boolean, default: false },
  idPanel: { type: String, required: true },
})

defineEmits(['alternar', 'seleccionar'])
</script>

<template>
  <div>
    <button
      type="button"
      class="flex w-full items-center justify-between gap-2 rounded-2xl px-4 py-3 text-left text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
      :aria-expanded="abierto"
      :aria-controls="idPanel"
      @click="$emit('alternar')"
    >
      <span class="flex items-center gap-2.5">
        <svg
          class="h-5 w-5 shrink-0"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M12 21a9 9 0 1 1 9-9c0 1.66-1.34 3-3 3h-1.5a1.5 1.5 0 0 0-1.06 2.56l.06.06A1.5 1.5 0 0 1 14.44 21H12Z" />
          <circle cx="7.5" cy="11.5" r="0.9" />
          <circle cx="12" cy="7.5" r="0.9" />
          <circle cx="16.5" cy="11.5" r="0.9" />
        </svg>
        Tema
      </span>
      <svg
        class="h-4 w-4 shrink-0 text-cafe/50 transition-transform"
        :class="abierto ? 'rotate-180' : ''"
        viewBox="0 0 20 20"
        fill="currentColor"
        aria-hidden="true"
      >
        <path
          fill-rule="evenodd"
          d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z"
          clip-rule="evenodd"
        />
      </svg>
    </button>

    <div v-if="abierto" :id="idPanel" class="px-2 pb-2 pt-1">
      <SelectorTema compacto @seleccionar="$emit('seleccionar')" />
    </div>
  </div>
</template>
