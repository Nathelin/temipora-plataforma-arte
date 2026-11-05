<script setup>
import { computed, ref, useId } from 'vue'
import { Info } from '@lucide/vue'

const props = defineProps({
  etiqueta: { type: String, default: 'Más información' },
  tono: { type: String, default: 'neutro' },
})

const id = useId()
const visible = ref(false)
const esDescuento = computed(() => props.tono === 'descuento')
const claseBoton = computed(() => (
  esDescuento.value
    ? 'text-coral hover:text-rojizo'
    : 'text-cafe hover:text-coral'
))
const claseTooltip = computed(() => (
  esDescuento.value
    ? 'border-coral/70 bg-granate text-white'
    : 'border-tinta bg-tinta text-white'
))

function mostrar() {
  visible.value = true
}

function ocultar() {
  visible.value = false
}

function alTeclear(event) {
  if (event.key === 'Escape') ocultar()
}
</script>

<template>
  <span
    class="relative inline-flex align-middle"
    @mouseenter="mostrar"
    @mouseleave="ocultar"
  >
    <button
      type="button"
      class="inline-flex h-5 w-5 cursor-help items-center justify-center rounded-full transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral"
      :class="claseBoton"
      :aria-label="etiqueta"
      :aria-describedby="visible ? id : undefined"
      @focus="mostrar"
      @blur="ocultar"
      @click="mostrar"
      @keydown.escape="alTeclear"
    >
      <Info class="h-4 w-4" aria-hidden="true" />
    </button>
    <span
      v-if="visible"
      :id="id"
      role="tooltip"
      class="absolute left-0 top-full z-50 mt-2 w-64 max-w-[calc(100vw-2rem)] whitespace-normal rounded-xl border px-3 py-2 text-left text-xs font-normal leading-relaxed shadow-xl"
      :class="claseTooltip"
    >
      <slot />
    </span>
  </span>
</template>
