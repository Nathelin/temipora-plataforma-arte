<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  imagenes: { type: Array, default: () => [] },
  indice: { type: Number, default: 0 },
  etiqueta: { type: String, default: '' },
  descripcion: { type: String, default: '' },
})

const emit = defineEmits(['cerrar', 'actualizar:indice'])

const indiceActual = ref(props.indice)
const total = computed(() => props.imagenes.length)
const urlActual = computed(() => props.imagenes[indiceActual.value] || '')

let overflowPrevio = ''
let scrollBloqueado = false

watch(
  () => props.abierto,
  (abierto) => {
    if (abierto) {
      indiceActual.value = props.indice
      if (!scrollBloqueado) {
        overflowPrevio = document.body.style.overflow
        scrollBloqueado = true
      }
      document.body.style.overflow = 'hidden'
    } else if (scrollBloqueado) {
      document.body.style.overflow = overflowPrevio
      scrollBloqueado = false
    }
  },
)

watch(
  () => props.indice,
  (valor) => {
    indiceActual.value = valor
  },
)

function seleccionar(indice) {
  indiceActual.value = indice
  emit('actualizar:indice', indice)
}

function mover(paso) {
  if (total.value < 2) return
  seleccionar((indiceActual.value + paso + total.value) % total.value)
}

function alPresionarTecla(evento) {
  if (!props.abierto) return
  if (evento.key === 'Escape') {
    evento.preventDefault()
    emit('cerrar')
  } else if (evento.key === 'ArrowRight') {
    evento.preventDefault()
    mover(1)
  } else if (evento.key === 'ArrowLeft') {
    evento.preventDefault()
    mover(-1)
  }
}

onMounted(() => window.addEventListener('keydown', alPresionarTecla))

onBeforeUnmount(() => {
  window.removeEventListener('keydown', alPresionarTecla)
  if (scrollBloqueado) document.body.style.overflow = overflowPrevio
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="abierto"
      class="fixed inset-0 z-[100] flex flex-col bg-granate/90 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label="Visor de imágenes"
      @click.self="emit('cerrar')"
    >
      <header class="flex shrink-0 items-start justify-between gap-4 px-4 py-3 text-white sm:px-6">
        <div class="min-w-0">
          <p class="truncate text-sm font-semibold">
            {{ etiqueta }}
            <span v-if="total"> — Imagen {{ indiceActual + 1 }}/{{ total }}</span>
          </p>
          <p v-if="descripcion" class="truncate text-xs text-white/70">{{ descripcion }}</p>
        </div>

        <button
          type="button"
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70"
          aria-label="Cerrar visor"
          @click="emit('cerrar')"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 6l12 12M18 6 6 18" />
          </svg>
        </button>
      </header>

      <div
        class="relative flex min-h-0 flex-1 items-center justify-center px-4 pb-2 sm:px-6"
        @click.self="emit('cerrar')"
      >
        <button
          v-if="total > 1"
          type="button"
          class="absolute left-2 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/25 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 sm:left-4"
          aria-label="Imagen anterior"
          @click="mover(-1)"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <img
          :src="urlActual"
          :alt="`${etiqueta} — imagen ${indiceActual + 1} de ${total}`"
          class="max-h-full max-w-full rounded-xl object-contain shadow-2xl shadow-black/40"
          @click.stop
        />

        <button
          v-if="total > 1"
          type="button"
          class="absolute right-2 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/25 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 sm:right-4"
          aria-label="Imagen siguiente"
          @click="mover(1)"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>

      <footer v-if="total > 1" class="flex shrink-0 justify-center gap-2 overflow-x-auto px-4 py-4">
        <button
          v-for="(url, i) in imagenes"
          :key="`${url}-${i}`"
          type="button"
          class="h-14 w-14 shrink-0 overflow-hidden rounded-lg border-2 transition"
          :class="i === indiceActual ? 'border-white' : 'border-white/20 opacity-60 hover:opacity-100'"
          :aria-label="`Ver imagen ${i + 1}`"
          :aria-current="i === indiceActual ? 'true' : undefined"
          @click="seleccionar(i)"
        >
          <img :src="url" alt="" class="h-full w-full object-cover" />
        </button>
      </footer>
    </div>
  </Teleport>
</template>
