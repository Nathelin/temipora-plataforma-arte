<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import SelectorTema from './SelectorTema.vue'

const abierto = ref(false)
const contenedor = ref(null)

function alternar() {
  abierto.value = !abierto.value
}

function cerrar() {
  abierto.value = false
}

function manejarClickExterior(event) {
  if (contenedor.value && !contenedor.value.contains(event.target)) {
    cerrar()
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape') {
    cerrar()
  }
}

onMounted(() => {
  document.addEventListener('click', manejarClickExterior)
  document.addEventListener('keydown', manejarTecla)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', manejarClickExterior)
  document.removeEventListener('keydown', manejarTecla)
})
</script>

<template>
  <div ref="contenedor" class="relative">
    <button
      type="button"
      class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-borde/60 bg-superficie/70 text-texto transition hover:bg-borde/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-acento/60 sm:h-10 sm:w-10"
      :aria-expanded="abierto"
      aria-controls="selector-tema-popover"
      aria-label="Elegir tema"
      title="Elegir tema"
      @click="alternar"
    >
      <svg
        class="h-5 w-5"
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
    </button>

    <div
      v-if="abierto"
      id="selector-tema-popover"
      class="absolute right-0 z-50 mt-2 w-[min(18rem,calc(100vw-2rem))] origin-top-right rounded-2xl border border-borde/60 bg-superficie p-3 shadow-[0_18px_40px_rgba(0,0,0,0.18)]"
      role="group"
      aria-label="Elegir tema"
    >
      <p class="px-1 pb-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-texto-suave">
        Apariencia
      </p>
      <SelectorTema compacto @seleccionar="cerrar" />
    </div>
  </div>
</template>
