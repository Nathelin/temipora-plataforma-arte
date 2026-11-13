<script setup>
import { computed } from 'vue'

const props = defineProps({
  eyebrow: { type: String, default: '' },
  titulo: { type: String, required: true },
  descripcion: { type: String, default: '' },
  nivel: { type: [Number, String], default: 1 },
})

const etiquetaTitulo = computed(() => {
  const valor = Number(props.nivel)
  return valor === 2 ? 'h2' : 'h1'
})
</script>

<template>
  <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
    <div class="min-w-0">
      <p
        v-if="eyebrow"
        class="text-sm font-semibold uppercase tracking-[0.18em] text-coral"
      >
        {{ eyebrow }}
      </p>
      <component
        :is="etiquetaTitulo"
        class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl"
      >
        {{ titulo }}
      </component>
      <p v-if="descripcion || $slots.descripcion" class="mt-2 max-w-2xl text-sm leading-relaxed text-cafe/65">
        <slot name="descripcion">{{ descripcion }}</slot>
      </p>
    </div>

    <div v-if="$slots.acciones" class="flex flex-wrap items-center gap-2">
      <slot name="acciones" />
    </div>
  </header>
</template>
