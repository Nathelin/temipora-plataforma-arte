<script setup>
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  cargando: { type: Boolean, default: false },
  error: { type: String, default: '' },
  maximo: { type: Number, default: 200 },
})

const emit = defineEmits(['cerrar', 'confirmar'])

const motivo = ref('')
const campoMotivo = ref(null)

watch(
  () => props.abierto,
  async (abierto) => {
    if (!abierto) return
    motivo.value = ''
    await nextTick()
    campoMotivo.value?.focus()
  },
)

function cerrar() {
  if (props.cargando) return
  emit('cerrar')
}

function confirmar() {
  if (props.cargando) return
  emit('confirmar', motivo.value.trim())
}
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-modal-rechazo-conversacion"
    @keydown.esc="cerrar"
  >
    <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="cerrar"></div>

    <div class="relative w-full max-w-md rounded-3xl border border-rosa bg-blanco p-6 shadow-xl shadow-granate/20">
      <h2 id="titulo-modal-rechazo-conversacion" class="font-display text-xl font-bold text-granate">
        Rechazar solicitud de mensaje
      </h2>
      <p class="mt-2 text-sm leading-relaxed text-cafe/70">
        No se va a abrir la conversación. La persona podrá volver a enviarte una
        solicitud cuando pase el plazo configurado.
      </p>

      <label for="motivo-rechazo-conversacion" class="mt-4 block text-sm font-medium text-cafe">
        Motivo <span class="font-normal text-cafe/50">(opcional)</span>
      </label>
      <textarea
        id="motivo-rechazo-conversacion"
        ref="campoMotivo"
        v-model="motivo"
        rows="3"
        :maxlength="maximo"
        placeholder="Si querés, contale por qué no vas a aceptar la solicitud..."
        class="mt-2 w-full resize-none rounded-2xl border border-rosa/70 bg-blanco px-3.5 py-2.5 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
      ></textarea>

      <div class="mt-1 flex items-start justify-between gap-3 text-[11px] text-cafe/50">
        <span>La otra persona va a poder leerlo en el detalle.</span>
        <span class="shrink-0">{{ motivo.length }}/{{ maximo }}</span>
      </div>

      <p
        v-if="error"
        class="mt-3 rounded-xl border border-rojizo/25 bg-rojizo/5 px-3.5 py-2.5 text-sm text-rojizo"
        role="alert"
      >
        {{ error }}
      </p>

      <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="min-h-10 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/20 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="cargando"
          @click="cerrar"
        >
          Volver
        </button>
        <button
          type="button"
          class="min-h-10 rounded-full bg-rojizo px-5 text-sm font-bold text-white shadow-sm transition hover:bg-granate disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="cargando"
          @click="confirmar"
        >
          {{ cargando ? 'Rechazando...' : 'Rechazar solicitud' }}
        </button>
      </div>
    </div>
  </div>
</template>
