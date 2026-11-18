<script setup>
import { useConfirmacionSalida } from '../composables/useConfirmacionSalida'

const {
  pendiente,
  mensaje,
  titulo,
  textoConfirmar,
  textoCancelar,
  textoAlternativo,
  procesandoAlternativa,
  errorAlternativa,
  responder,
  responderAlternativa,
} = useConfirmacionSalida()
</script>

<template>
  <div
    v-if="pendiente"
    class="fixed inset-0 z-[80] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-confirmacion-salida"
  >
    <div
      class="absolute inset-0 bg-granate/40"
      aria-hidden="true"
      @click="responder(false)"
    ></div>

    <div class="relative w-full max-w-md bg-blanco rounded-xl shadow-xl shadow-granate/20 border border-rosa p-6">
      <h2 id="titulo-confirmacion-salida" class="text-lg text-granate">{{ titulo }}</h2>
      <p class="mt-2 text-sm text-cafe/70">{{ mensaje }}</p>

      <p
        v-if="errorAlternativa"
        class="mt-3 rounded-lg border border-rojizo/30 bg-rojizo/5 px-3 py-2 text-sm text-rojizo"
        role="alert"
      >
        {{ errorAlternativa }}
      </p>

      <div class="mt-6 flex flex-col-reverse sm:flex-row sm:justify-end gap-3">
        <button
          v-if="textoAlternativo"
          type="button"
          :disabled="procesandoAlternativa"
          class="px-4 py-2 rounded-md text-sm text-granate border border-coral/40 bg-coral/10 hover:bg-coral/20 transition sm:mr-auto disabled:opacity-60"
          @click="responderAlternativa"
        >
          {{ procesandoAlternativa ? 'Guardando…' : textoAlternativo }}
        </button>
        <button
          type="button"
          class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
          @click="responder(true)"
        >
          {{ textoConfirmar }}
        </button>
        <button
          type="button"
          class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
          @click="responder(false)"
        >
          {{ textoCancelar }}
        </button>
      </div>
    </div>
  </div>
</template>
