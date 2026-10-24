<script setup>
defineProps({
  abierto: { type: Boolean, default: false },
  titulo: { type: String, default: '¿Confirmás la acción?' },
  mensaje: { type: String, default: '' },
  textoConfirmar: { type: String, default: 'Confirmar' },
  textoCancelar: { type: String, default: 'Cancelar' },
  destructivo: { type: Boolean, default: false },
})

defineEmits(['confirmar', 'cerrar'])
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-modal-confirmacion"
  >
    <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="$emit('cerrar')"></div>

    <div class="relative w-full max-w-md rounded-xl border border-rosa bg-blanco p-6 shadow-xl shadow-granate/20">
      <h2 id="titulo-modal-confirmacion" class="text-lg text-granate">{{ titulo }}</h2>
      <p v-if="mensaje" class="mt-2 text-sm leading-relaxed text-cafe/70">{{ mensaje }}</p>
      <slot />

      <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
          @click="$emit('cerrar')"
        >
          {{ textoCancelar }}
        </button>
        <button
          type="button"
          class="rounded-md px-5 py-2.5 text-sm text-white shadow-sm transition"
          :class="destructivo ? 'bg-rojizo hover:bg-granate' : 'bg-coral hover:bg-rojizo'"
          @click="$emit('confirmar')"
        >
          {{ textoConfirmar }}
        </button>
      </div>
    </div>
  </div>
</template>
