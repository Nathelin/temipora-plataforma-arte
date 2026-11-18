<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId } from 'vue'
import { Info } from '@lucide/vue'

const props = defineProps({
  etiqueta: { type: String, default: 'Más información' },
  tono: { type: String, default: 'neutro' },
})

const MARGEN = 16

const id = useId()
const visible = ref(false)
const alinearDerecha = ref(false)
const abrirArriba = ref(false)
const contenedor = ref(null)
const burbuja = ref(null)

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

function posicionar() {
  const cont = contenedor.value
  const tooltip = burbuja.value
  if (!cont || !tooltip) return
  const rect = cont.getBoundingClientRect()
  const ancho = tooltip.offsetWidth
  const alto = tooltip.offsetHeight

  const espacioDerecha = window.innerWidth - rect.left - MARGEN
  const espacioIzquierda = rect.right - MARGEN
  alinearDerecha.value = espacioDerecha < ancho && espacioIzquierda > espacioDerecha

  const espacioAbajo = window.innerHeight - rect.bottom - MARGEN
  const espacioArriba = rect.top - MARGEN
  abrirArriba.value = espacioAbajo < alto && espacioArriba > espacioAbajo
}

async function mostrar() {
  if (visible.value) return
  visible.value = true
  abrirArriba.value = false
  await nextTick()
  posicionar()
}

function ocultar() {
  visible.value = false
}

function alTeclear(event) {
  if (event.key === 'Escape') ocultar()
}

function alRedimensionar() {
  posicionar()
}

function alMostrar() {
  window.addEventListener('resize', alRedimensionar)
  mostrar()
}

function alOcultar() {
  window.removeEventListener('resize', alRedimensionar)
  ocultar()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', alRedimensionar)
})
</script>

<template>
  <span
    ref="contenedor"
    class="relative inline-flex align-middle"
    @mouseenter="alMostrar"
    @mouseleave="alOcultar"
  >
    <button
      type="button"
      class="inline-flex h-5 w-5 cursor-help items-center justify-center rounded-full transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral"
      :class="claseBoton"
      :aria-label="etiqueta"
      :aria-describedby="visible ? id : undefined"
      @focus="alMostrar"
      @blur="alOcultar"
      @click="mostrar"
      @keydown.escape="alTeclear"
    >
      <Info class="h-4 w-4" aria-hidden="true" />
    </button>
    <span
      ref="burbuja"
      v-if="visible"
      :id="id"
      role="tooltip"
      class="absolute z-50 w-64 max-w-[calc(100vw-2rem)] whitespace-normal rounded-xl border px-3 py-2 text-left text-xs font-normal leading-relaxed shadow-xl"
      :class="[
        claseTooltip,
        alinearDerecha ? 'right-0' : 'left-0',
        abrirArriba ? 'bottom-full mb-2' : 'top-full mt-2',
      ]"
    >
      <slot />
    </span>
  </span>
</template>
