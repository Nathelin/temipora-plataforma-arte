<script setup>
import { ref } from 'vue'

const props = defineProps({
  titulo: { type: String, required: true },
  abierto: { type: Boolean, default: false },
  activos: { type: Number, default: 0 },
})

const expandido = ref(props.abierto)
</script>

<template>
  <section class="border-b border-borde/40 py-3 last:border-b-0">
    <button
      type="button"
      class="flex w-full items-center justify-between gap-2 text-left"
      :aria-expanded="expandido"
      @click="expandido = !expandido"
    >
      <span class="flex items-center gap-2">
        <span class="text-sm font-semibold text-granate">{{ titulo }}</span>
        <span
          v-if="activos > 0"
          class="rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-bold leading-none text-white"
        >
          {{ activos }}
        </span>
      </span>
      <svg
        class="h-4 w-4 shrink-0 text-cafe/50 transition-transform"
        :class="expandido ? 'rotate-180' : ''"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        aria-hidden="true"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
      </svg>
    </button>

    <div v-show="expandido" class="mt-3">
      <slot />
    </div>
  </section>
</template>
